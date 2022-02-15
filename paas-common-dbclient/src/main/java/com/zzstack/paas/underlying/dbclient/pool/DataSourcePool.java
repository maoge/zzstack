package com.zzstack.paas.underlying.dbclient.pool;

import java.sql.Connection;

import javax.sql.DataSource;

import com.zzstack.paas.underlying.dbclient.exception.DBException;

public interface DataSourcePool extends AutoCloseable {

	/**
	 * 从连接池中获取数据库连接
	 * @return
	 */
	public Connection getConnection() throws DBException;
	
	/**
	 * 暴漏出DataSource,便于和iBatis集成
	 * @return
	 */
	public DataSource getDataSource();
	
	/**
	 * 使用完归还数据库连接
	 * 
	 * @param conn 数据库连接
	 */
	public void recycle(Connection conn);
	
	public boolean check();
	
	/**
	 * 获取连接池名
	 * 
	 * @return
	 */
	public String getName();
	
	public String getDBInfo();

}
