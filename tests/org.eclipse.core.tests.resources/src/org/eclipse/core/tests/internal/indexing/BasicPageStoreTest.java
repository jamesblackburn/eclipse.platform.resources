/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.indexing;

import java.util.Random;
import junit.framework.*;
import org.eclipse.core.internal.indexing.PageStore;

public class BasicPageStoreTest extends AbstractIndexedStoreTest{

	public BasicPageStoreTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(BasicPageStoreTest.class);
	}

	// check a byte array against a value
	boolean check(byte[] b, byte i) {
		for (int j = 0; j < b.length; j++) {
			if (b[j] != i)
				return false;
		}
		return true;
	}

	// fill a byte array with a value
	void fill(byte[] b, byte i) {
		for (int j = 0; j < b.length; j++)
			b[j] = i;
	}

	/**
	 * Creates an initialized 128 page store.
	 */
	public int initializeStore() throws Exception {
		PageStore.delete(getFileName());
		PageStore store = new PageStore(new TestPagePolicy());
		store.open(getFileName());
		int n = 128;
		for (int i = 0; i < n; i++) {
			TestPage p = (TestPage) store.acquire(i);
			p.fill((byte) i);
			p.release();
		}
		store.close();
		return n;
	}

	void printStats(PageStore store) throws Exception {
		println("Number of pages       = " + store.numberOfPages());
		println("Number of writes      = " + store.numberOfFileWrites());
		println("Number of file reads  = " + store.numberOfFileReads());
		println("Number of cache reads = " + store.numberOfCacheHits());
		println("Number of reads       = " + store.numberOfReads());
		println("Cache hit ratio       = " + (float) store.numberOfCacheHits() / (float) store.numberOfReads());
	}

	/**
	 * Test cache performance using a circular reference pattern.
	 */
	public void testCacheHitsCircular() throws Exception {
		printHeading("testCacheHitsCircular");
		initializeStore();
		PageStore store = new PageStore(new TestPagePolicy());
		println("Testing 41 of 40");
		store.open(getFileName());
		for (int j = 0; j < 100; j++) {
			for (int i = 0; i < 41; i++) {
				TestPage p = (TestPage) store.acquire(i);
				assertTrue(p.check((byte) i));
				p.release();
			}
		}
		printStats(store);
		store.close();
		println("Testing 40 of 40");
		store.open(getFileName());
		for (int j = 0; j < 100; j++) {
			for (int i = 0; i < 40; i++) {
				TestPage p = (TestPage) store.acquire(i);
				assertTrue(p.check((byte) i));
				p.release();
			}
		}
		printStats(store);
		store.close();
	}

	/**
	 * Test the effect of increasing cache sizes
	 */
	public void testCacheHitsRandom() throws Exception {
		printHeading("testCacheHitsRandom");
		PageStore.delete(getFileName());
		int n = initializeStore();
		for (int m = 0; m <= n; m += 16) {
			PageStore store = new PageStore(new TestPagePolicy());
			store.open(getFileName());
			Random r = new Random(100);
			for (int i = 0; i < 1000; i++) {
				TestPage p = (TestPage) store.acquire(Math.abs(r.nextInt() % n));
				p.release();
			}
			printStats(store);
			store.close();
		}
	}

	/**
	 * Checks the performance of sequential access.
	 */
	public void testCacheHitsSequential() throws Exception {
		printHeading("testCacheHitsSequential");
		int n = initializeStore();
		PageStore store = new PageStore(new TestPagePolicy());
		store.open(getFileName());
		for (int i = 0; i < n; i++) {
			TestPage p = (TestPage) store.acquire(i);
			assertTrue(p.check((byte) i));
			p.release();
		}
		printStats(store);
		store.close();
	}

	/**
	 */
	public void testCreate() throws Exception {
		printHeading("testCreate");
		PageStore.create(getFileName());
		assertTrue(PageStore.exists(getFileName()));
	}

	/**
	 */
	public void testDelete() throws Exception {
		printHeading("testDelete");
		PageStore.delete(getFileName());
		assertTrue(!PageStore.exists(getFileName()));
	}

	/**
	 * Tests the log.
	 */
	public void testLogging1() throws Exception {
		printHeading("testLogging1");
		PageStore.delete(getFileName());
		PageStore store = new PageStore(new TestPagePolicy());
		store.open(getFileName());
		testLogPopulate(store);
		store.testLogging1();
		testLogValidate(store);
		store.close();
	}

	/**
	 * Tests the log.
	 */
	public void testLogging2() throws Exception {
		printHeading("testLogging2");
		PageStore.delete(getFileName());
		PageStore store = new PageStore(new TestPagePolicy());
		store.open(getFileName());
		testLogPopulate(store);
		store.testLogging2();
		testLogValidate(store);
		store.close();
	}

	/**
	 * Tests the log.
	 */
	public void testLogging3() throws Exception {
		printHeading("testLogging3");
		PageStore.delete(getFileName());
		PageStore store = new PageStore(new TestPagePolicy());
		store.open(getFileName());
		testLogPopulate(store);
		store.testLogging3();
		testLogValidate(store);
		store.close();
	}

	/**
	 * Populate the store for the logging tests.
	 */
	public void testLogPopulate(PageStore store) throws Exception {
		for (int i = 0; i < 128; i++) {
			TestPage p = (TestPage) store.acquire(i);
			p.fill((byte) i);
			p.release();
		}
	}

	/**
	 * Tests the contents of the store for the logging tests.
	 */
	public void testLogValidate(PageStore store) throws Exception {
		for (int i = 0; i < 128; i++) {
			TestPage p = (TestPage) store.acquire(i);
			assertTrue("Failed checking page " + i, p.check((byte) i));
			p.release();
		}
	}

	/**
	 * Tests random reading & writing.
	 */
	public void testRandomReadWrite() throws Exception {
		printHeading("testRandomReadWrite");
		PageStore.delete(getFileName());
		int n = 128;
		byte[] value = new byte[n];
		for (int i = 0; i < n; i++)
			value[i] = 0;
		PageStore store = new PageStore(new TestPagePolicy());
		store.open(getFileName());
		Random r = new Random(100);
		for (int i = 0; i < 2000; i++) {
			int k = Math.abs(r.nextInt() % n);
			TestPage p = (TestPage) store.acquire(k);
			assertTrue(p.check(value[k]));
			value[k] = (byte) r.nextInt();
			p.fill(value[k]);
			p.release();
		}
		printStats(store);
		store.close();
	}

	/**
	 * Tests read-only access on the store.
	 */
	public void testReadOnly() throws Exception {
		printHeading("testReadOnly");
		PageStore.delete(getFileName());
		int n = initializeStore();
		PageStore store = new PageStore(new TestPagePolicy());
		store.open(getFileName());
		assertTrue(store.numberOfPages() == n);
		for (int i = 0; i < n; i++) {
			TestPage p = (TestPage) store.acquire(i);
			assertTrue(p.check((byte) i));
			p.release();
		}
		printStats(store);
		store.close();
	}

	/**
	 * Adds & checks 128 8K pages (1 meg) to the page file.
	 */
	public void testWrite() throws Exception {
		printHeading("testWrite");
		PageStore.delete(getFileName());
		PageStore store = new PageStore(new TestPagePolicy());
		store.open(getFileName());
		writeBlock(store);
		printStats(store);
		store.close();
	}

	/**
	 * Adds a 64 meg chunk to the page file.
	 */
	public void testWriteHuge() throws Exception {
		printHeading("testWriteHuge");
		PageStore.delete(getFileName());
		PageStore store = new PageStore(new TestPagePolicy());
		store.open(getFileName());
		for (int i = 0; i < 64; i++)
			writeBlock(store);
		printStats(store);
		store.close();
	}

	/**
	 * Adds a 16 meg chunk to the page file.
	 */
	public void testWriteLarge() throws Exception {
		printHeading("testWriteLarge");
		PageStore.delete(getFileName());
		PageStore store = new PageStore(new TestPagePolicy());
		store.open(getFileName());
		for (int i = 0; i < 16; i++)
			writeBlock(store);
		printStats(store);
		store.close();
	}

	/**
	 * Adds & checks 128 8K pages (1 meg) to the page file.
	 */
	public int writeBlock(PageStore store) throws Exception {
		TestPage p = null;
		int m = 128;
		int n1 = store.numberOfPages();
		int n2 = n1 + m;
		for (int i = n1; i < n2; i++) {
			p = (TestPage) store.acquire(i);
			p.fill((byte) i);
			p.release();
		}
		store.commit();
		assertEquals(store.numberOfPages(), n2);
		for (int i = n1; i < n2; i++) {
			p = (TestPage) store.acquire(i);
			assertTrue("Page " + i + " " + p.value(), p.check((byte) i));
			p.release();
		}
		return m;
	}
}