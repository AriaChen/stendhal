/* $Id$ */
/***************************************************************************
 *                   (C) Copyright 2003-2010 - Stendhal                    *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package games.stendhal.tools.loganalyser.util;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createStrictMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;

import org.junit.Test;

/**
 * Test for the result set iterator
 */
public class ResultSetIteratorTest {
	ResultSet resultSet = createMock(ResultSet.class);

	/**
	 * Returns false for boolean, null for object and 0 for numbers.
	 *
	 */
	private class StatementImplementation implements Statement {
		@Override
		public void addBatch(final String sql) throws SQLException {
			// Stub
		}

		@Override
		public void cancel() throws SQLException {
			// Stub
		}

		@Override
		public void clearBatch() throws SQLException {
			// Stub
		}

		@Override
		public void clearWarnings() throws SQLException {
			// Stub
		}

		@Override
		public void close() throws SQLException {
			// Stub
		}

		@Override
		public boolean execute(final String sql) throws SQLException {
			// Stub
			return false;
		}

		@Override
		public boolean execute(final String sql, final int autoGeneratedKeys) throws SQLException {
			// Stub
			return false;
		}

		@Override
		public boolean execute(final String sql, final int[] columnIndexes) throws SQLException {
			// Stub
			return false;
		}

		@Override
		public boolean execute(final String sql, final String[] columnNames) throws SQLException {
			// Stub
			return false;
		}

		@Override
		public int[] executeBatch() throws SQLException {
			// Stub
			return null;
		}

		@Override
		public ResultSet executeQuery(final String sql) throws SQLException {
			// Stub
			return null;
		}

		@Override
		public int executeUpdate(final String sql) throws SQLException {
			// Stub
			return 0;
		}

		@Override
		public int executeUpdate(final String sql, final int autoGeneratedKeys) throws SQLException {
			// Stub
			return 0;
		}

		@Override
		public int executeUpdate(final String sql, final int[] columnIndexes) throws SQLException {
			// Stub
			return 0;
		}

		@Override
		public int executeUpdate(final String sql, final String[] columnNames) throws SQLException {
			// Stub
			return 0;
		}

		@Override
		public Connection getConnection() throws SQLException {
			// Stub
			return null;
		}

		@Override
		public int getFetchDirection() throws SQLException {
			// Stub
			return 0;
		}

		@Override
		public int getFetchSize() throws SQLException {
			// Stub
			return 0;
		}

		@Override
		public ResultSet getGeneratedKeys() throws SQLException {
			// Stub
			return null;
		}

		@Override
		public int getMaxFieldSize() throws SQLException {
			// Stub
			return 0;
		}

		@Override
		public int getMaxRows() throws SQLException {
			// Stub
			return 0;
		}

		@Override
		public boolean getMoreResults() throws SQLException {
			// Stub
			return false;
		}

		@Override
		public boolean getMoreResults(final int current) throws SQLException {
			// Stub
			return false;
		}

		@Override
		public int getQueryTimeout() throws SQLException {
			// Stub
			return 0;
		}

		@Override
		public ResultSet getResultSet() throws SQLException {
			// Stub
			return null;
		}

		@Override
		public int getResultSetConcurrency() throws SQLException {
			// Stub
			return 0;
		}

		@Override
		public int getResultSetHoldability() throws SQLException {
			// Stub
			return 0;
		}

		@Override
		public int getResultSetType() throws SQLException {
			// Stub
			return 0;
		}

		@Override
		public int getUpdateCount() throws SQLException {
			// Stub
			return 0;
		}

		@Override
		public SQLWarning getWarnings() throws SQLException {
			// Stub
			return null;
		}

		@Override
		public boolean isClosed() throws SQLException {
			// Stub
			return false;
		}

		@Override
		public boolean isPoolable() throws SQLException {
			// Stub
			return false;
		}

		@Override
		public void setCursorName(final String name) throws SQLException {
			// Stub
		}

		@Override
		public void setEscapeProcessing(final boolean enable) throws SQLException {
			// Stub
		}

		@Override
		public void setFetchDirection(final int direction) throws SQLException {
			// Stub
		}

		@Override
		public void setFetchSize(final int rows) throws SQLException {
			// Stub
		}

		@Override
		public void setMaxFieldSize(final int max) throws SQLException {
			// Stub
		}

		@Override
		public void setMaxRows(final int max) throws SQLException {
			// Stub
		}

		@Override
		public void setPoolable(final boolean poolable) throws SQLException {
			// Stub
		}

		@Override
		public void setQueryTimeout(final int seconds) throws SQLException {
			// Stub
		}

		@Override
		public boolean isWrapperFor(final Class< ? > iface) throws SQLException {
			// Stub
			return false;
		}

		@Override
		public <T> T unwrap(final Class<T> iface) throws SQLException {
			// Stub
			return null;
		}

		public void closeOnCompletion() throws SQLException {
			// Stub
		}

		public boolean isCloseOnCompletion() throws SQLException {
			// Stub
			return false;
		}
	}

	/**
	 * Returns null for createObject().
	 *
	 */
	private static class ResultSetIterImplementation extends ResultSetIterator<String> {
		private ResultSetIterImplementation(final Statement statement, final ResultSet resultSet) {
			super(statement, resultSet);
		}

		@Override
		protected String createObject() {
			return null;
		}
	}

	/**
	 * Tests for resultSetIterator.
	 */
	@Test
	public void testResultSetIterator() {

		new ResultSetIterImplementation(new StatementImplementation(), resultSet);

	}

	/**
	 * Tests for createObject.
	 */
	@Test
	public void testCreateObject() {
		final ResultSetIterator<String> iter = new ResultSetIterImplementation(new StatementImplementation(),
																		resultSet) {
			@Override
			protected String createObject() {
				return "result";
			}

		};
		assertThat(iter.createObject(), is("result"));
	}

	/**
	 * Tests for hasNext.
	 *
	 * @throws SQLException in case of an unexpected problem
	 */
	@Test
	public void testHasNext() throws SQLException {

		ResultSet resulthasnext = createStrictMock(ResultSet.class);

		expect(resulthasnext.next()).andReturn(false);
		resulthasnext.close();
		expect(resulthasnext.next()).andReturn(false);
		resulthasnext.close();

		replay(resulthasnext);
		ResultSetIterator<String> iter = new ResultSetIterImplementation(new StatementImplementation(),
				resulthasnext);

		assertFalse(iter.hasNext());
		assertFalse(iter.hasNext());


		ResultSet resultNothasnext = createStrictMock(ResultSet.class);

		expect(resultNothasnext.next()).andReturn(true);
		resultNothasnext.close();
		expect(resultNothasnext.next()).andReturn(true);
		resultNothasnext.close();
		replay(resultNothasnext);

		iter = new ResultSetIterImplementation(new StatementImplementation(), resultNothasnext);
		assertTrue(iter.hasNext());
		assertTrue(iter.hasNext());
		iter = new ResultSetIterImplementation(new StatementImplementation(), resulthasnext);
		assertFalse(iter.hasNext());
		assertFalse(iter.hasNext());
	}

	/**
	 * Tests for next.
	 *
	 * @throws SQLException in case of an unexpected problem
	 */
	@Test
	public void testNext() throws SQLException {
		ResultSet localResultSet = createMock(ResultSet.class);
		expect(localResultSet.next()).andReturn(true).times(2);

		replay(localResultSet);
		final ResultSetIterator<String> iter = new ResultSetIterImplementation(new StatementImplementation(),
																		localResultSet) {
			protected String object = "";

			@Override
			protected String createObject() {
				object = object + "a";
				return object;
			}

			@Override
			public boolean hasNext() {
				return true;
			}
		};
		assertTrue(iter.hasNext());
		assertThat(iter.next(), is("a"));
		assertTrue(iter.hasNext());
		assertThat(iter.next(), is("aa"));
	}


	/**
	 * Tests for close.
	 *
	 * @throws SQLException in case of an unexpected problem
	 */
	@Test
	public void testClose() throws SQLException {
		ResultSet resultSetClose = createMock(ResultSet.class);
		Statement statement = createMock(Statement.class);
		resultSetClose.close();
		expect(resultSetClose.next()).andReturn(false);
		statement.close();

		replay(resultSetClose);
		replay(statement);

		final ResultSetIterator<String> iter = new ResultSetIterImplementation(statement, resultSetClose);
		iter.next();
		verify(resultSetClose);
		verify(statement);
	}

	/**
	 * Tests for remove.
	 *
	 * @throws SQLException in case of an unexpected problem
	 */
	@Test
	public void testRemove() throws SQLException {
		ResultSet resultsetdelteThrowsException = createMock(ResultSet.class);
		resultsetdelteThrowsException.deleteRow();
		expectLastCall().andThrow(new SQLException());
		replay(resultsetdelteThrowsException);
		final ResultSetIterator<String> iter = new ResultSetIterImplementation(new StatementImplementation(),
				resultsetdelteThrowsException);
		iter.remove();
		assertTrue("no exception thrown", true);
		verify(resultsetdelteThrowsException);
	}

	/**
	 * Tests for iterator.
	 */
	@Test
	public void testIterator() {
		final ResultSetIterator<String> iter = new ResultSetIterImplementation(new StatementImplementation(),
																		resultSet);
		assertSame(iter, iter.iterator());
	}

}
