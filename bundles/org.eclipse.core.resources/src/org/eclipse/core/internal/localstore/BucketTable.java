/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.localstore;

import java.io.*;
import java.util.*;
import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.internal.utils.UniversalUniqueIdentifier;
import org.eclipse.core.runtime.*;

public class BucketTable {

	public static final class Entry {
		public final static int LONG_LENGTH = 8;
		public final static int UUID_LENGTH = UniversalUniqueIdentifier.BYTES_SIZE;
		public final static int DATA_LENGTH = UUID_LENGTH + LONG_LENGTH;		
		byte[][] data;
		IPath path;

		public static byte[] getDataAsByteArray(byte[] uuid, long timestamp) {
			byte[] data = new byte[DATA_LENGTH];
			System.arraycopy(uuid, 0, data, 0, uuid.length);
			for (int j = 0; j < LONG_LENGTH; j++)
				data[UUID_LENGTH + j] = (byte) (timestamp >>> j * 8);
			return data;
		}

		Entry(IPath path, byte[][] data) {
			this.path = path;
			this.data = data;
		}
		/**
		 * Compacts the given array removing any null slots.
		 */
		void compact() {
			if(data == null)
				return;
			int occurrences = 0;
			for (int i = 0; i < data.length; i++)
				if (data[i] != null)
					data[occurrences++] = data[i];
			if (occurrences == data.length)
				// no items deleted
				return;
			if (occurrences == 0) {
				// no items remaining
				data = null;
				return;
			}
			byte[][] result = new byte[occurrences][];
			System.arraycopy(data, 0, result, 0, occurrences);
			data = result;
		}
		
		public void deleteOccurrence(int i) {
			data[i] = null;
		}

		public byte[][] getData() {
			return getData(false);
		}

		public byte[][] getData(boolean clone) {
			if (!clone)
				return data == null ? new byte[0][] : data;
			// don't need to clone the contained arrays because they immutable
			byte[][] newData = new byte[data.length][];
			System.arraycopy(data, 0, newData, 0, data.length);
			return newData;
		}

		int getOccurrences() {
			return data == null ? 0 : data.length;
		}

		public IPath getPath() {
			return path;
		}

		public long getTimestamp(int i) {			
			long timestamp = 0;
			for (int j = 0; j < LONG_LENGTH; j++)
				timestamp += (data[i][UUID_LENGTH + j] & 0xFF) << j * 8;
			return timestamp;
		}

		public UniversalUniqueIdentifier getUUID(int i) {
			return new UniversalUniqueIdentifier(data[i]);
		}

		public boolean isEmpty() {
			return data == null || data.length == 0;
		}

	}

	public abstract static class Visitor {

		// should stop the traversal
		public final static int CONTINUE = 0;
		// should delete this entry (can be combined with the other constants except for UPDATE)
		public final static int DELETE = 0x100;
		// should stop looking at states for files in this container (or any of its children)	
		public final static int RETURN = 2;
		// keep visiting, still happy	
		public final static int STOP = 1;
		// should update this entry (can be combined with the other constants except for DELETE)		
		public final static int UPDATE = 0x200;

		public void newBucket() {
			// don't do anything
		}

		/** 
		 * @return either STOP, CONTINUE or RETURN and optionally DELETE
		 */
		public abstract int visit(Entry entry);
	}

	private static final String BUCKET = ".bucket"; //$NON-NLS-1$

	private final static byte VERSION = 1;

	//	private static final int UUID_LENGTH = new UniversalUniqueIdentifier().toString().length();

	//	private static RecyclableBufferedInputStream bufferedInputStream = new RecyclableBufferedInputStream();

	//	private static RecyclableBufferedOutputStream bufferedOutputStream = new RecyclableBufferedOutputStream();
	private Map entries;
	private File location;
	private boolean needSaving = false;

	private File root;

	private static int indexOf(byte[][] array, byte[] item) {
		// look for existing occurrences
		for (int i = 0; i < array.length; i++)
			if (UniversalUniqueIdentifier.equals(item, array[i]))
				return i;
		return -1;
	}

	public BucketTable(File root) {
		this.root = root;
		this.entries = new HashMap();
	}

	/**
	 * 
	 * @param visitor
	 * @param filter
	 * @return one of STOP, RETURN or CONTINUE constants
	 * @throws CoreException
	 */
	public int accept(Visitor visitor, IPath filter, boolean exactMatch, boolean sorted) throws CoreException {
		if (entries.isEmpty())
			return Visitor.CONTINUE;
		try {
			visitor.newBucket();
			for (Iterator i = entries.entrySet().iterator(); i.hasNext();) {
				Map.Entry entry = (Map.Entry) i.next();
				IPath path = new Path((String) entry.getKey());
				// check whether the filter applies
				if (!filter.isPrefixOf(path) || (exactMatch && !filter.equals(path)))
					continue;
				// calls the visitor passing all uuids for the entry
				final Entry fileEntry = new Entry(path, (byte[][]) entry.getValue());
				int outcome = visitor.visit(fileEntry);
				if ((outcome & Visitor.UPDATE) != 0) {
					needSaving = true;
					fileEntry.compact();
					if (fileEntry.isEmpty())
						i.remove();
					else
						entry.setValue(fileEntry.getData());
				} else if ((outcome & Visitor.DELETE) != 0) {
					needSaving = true;
					i.remove();
				}
				if ((outcome & Visitor.RETURN) != 0)
					// skip any other buckets under this
					return Visitor.RETURN;
				if ((outcome & Visitor.STOP) != 0)
					// stop looking
					return Visitor.STOP;
			}
			return Visitor.CONTINUE;
		} finally {
			save();
		}
	}

	public void addBlob(IPath path, byte[] uuid, long lastModified) {
		String pathAsString = path.toString();
		byte[][] existing = (byte[][]) entries.get(pathAsString);
		if (existing == null) {
			entries.put(pathAsString, new byte[][] {Entry.getDataAsByteArray(uuid, lastModified)});
			needSaving = true;
			return;
		}
		// look for existing occurrences
		if (contains(existing, uuid))
			// already there - nothing else to be done
			return;
		byte[][] newValue = new byte[existing.length + 1][];
		System.arraycopy(existing, 0, newValue, 0, existing.length);
		newValue[newValue.length - 1] = Entry.getDataAsByteArray(uuid, lastModified);
		sortUUIDs(newValue);
		entries.put(pathAsString, newValue);
		needSaving = true;
	}

	public void addBlobs(Entry fileEntry) {
		IPath path = fileEntry.getPath();
		byte[][] data = fileEntry.getData();
		String pathAsString = path.toString();
		byte[][] existing = (byte[][]) entries.get(pathAsString);
		if (existing == null) {
			entries.put(pathAsString, data);
			needSaving = true;
			return;
		}
		// add after looking for existing occurrences
		List newUUIDs = new ArrayList(existing.length + data.length);
		for (int i = 0; i < data.length; i++)
			if (!contains(existing, data[i]))
				newUUIDs.add(data[i]);
		if (newUUIDs.isEmpty())
			// none added
			return;
		byte[][] newValue = new byte[existing.length + newUUIDs.size()][];
		newUUIDs.toArray(newValue);
		System.arraycopy(existing, 0, newValue, newUUIDs.size(), existing.length);
		entries.put(pathAsString, newValue);
		needSaving = true;
	}

	private boolean contains(byte[][] array, byte[] item) {
		return indexOf(array, item) >= 0;
	}

	/**
	 * Tries to delete as many empty levels as possible.
	 */
	private void delete(File toDelete) {
		// don't try to delete the root for bucket indexes
		if (toDelete.equals(root))
			return;
		if (toDelete.delete())
			// if deletion went fine, try deleting the parent dir			
			delete(toDelete.getParentFile());
	}

	public Entry getEntry(IPath path) {
		String pathAsString = path.toString();
		byte[][] existing = (byte[][]) entries.get(pathAsString);
		if (existing == null)
			return new Entry(path, null);
		sortUUIDs(existing);
		return new Entry(path, existing);
	}

	File getLocation() {
		return location == null ? null : location.getParentFile();
	}

	public void load(File baseLocation) throws CoreException {
		try {
			// avoid reloading
			if (this.location != null && baseLocation.equals(this.location.getParentFile()))
				return;
			// previously loaded bucket may not have been saved... save before loading new one
			save();
			this.location = new File(baseLocation, BUCKET);
			this.entries.clear();
			if (!this.location.isFile())
				return;
			DataInputStream source = new DataInputStream(new BufferedInputStream(new FileInputStream(location), 8192));
			try {
				if (source.readByte() != VERSION)
					// TODO proper error handling here
					throw new IOException("Wrong version");
				int entryCount = source.readInt();
				for (int i = 0; i < entryCount; i++) {
					String key = source.readUTF();
					int length = source.readUnsignedShort();
					byte[][] uuids = new byte[length][Entry.DATA_LENGTH];
					for (int j = 0; j < uuids.length; j++)
						source.read(uuids[j]);
					this.entries.put(key, uuids);
				}
			} finally {
				source.close();
			}
		} catch (IOException ioe) {
			//TODO
			throw new ResourceException(0, null, "", ioe);
		}
	}

	public void save() throws CoreException {
		if (!needSaving)
			return;
		try {
			if (entries.isEmpty()) {
				needSaving = false;
				delete(location);
				return;
			}
			// ensure the parent location exists 
			location.getParentFile().mkdirs();
			DataOutputStream destination = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(location), 8192));
			try {
				destination.write(VERSION);
				destination.writeInt(entries.size());
				for (Iterator i = entries.entrySet().iterator(); i.hasNext();) {
					Map.Entry entry = (Map.Entry) i.next();
					destination.writeUTF((String) entry.getKey());
					byte[][] uuids = (byte[][]) entry.getValue();
					destination.writeShort(uuids.length);
					for (int j = 0; j < uuids.length; j++)
						destination.write(uuids[j]);
				}
			} finally {
				destination.close();
			}
			needSaving = false;
		} catch (IOException ioe) {
			throw new ResourceException(0, null, "", ioe);
		}
	}

	private void sortUUIDs(byte[][] uuids) {
		Arrays.sort(uuids, new Comparator() {
			public int compare(Object o1, Object o2) {
				return -UniversalUniqueIdentifier.compareTime((byte[]) o1, (byte[]) o2);
			}
		});
	}
}
