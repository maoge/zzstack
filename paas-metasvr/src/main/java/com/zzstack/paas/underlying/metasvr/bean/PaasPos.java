package com.zzstack.paas.underlying.metasvr.bean;

import com.zzstack.paas.underlying.utils.consts.CONSTS;

public class PaasPos {
	
	private int x = 0;
	private int y = 0;
	private int width = CONSTS.POS_DEFAULT_VALUE;
	private int height = CONSTS.POS_DEFAULT_VALUE;
	private int row = CONSTS.POS_DEFAULT_VALUE;
	private int col = CONSTS.POS_DEFAULT_VALUE;
	
	public PaasPos() {
		super();
	}
	
	/**
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @param row
	 * @param col
	 */
	public PaasPos(int x, int y, int width, int height, int row, int col) {
		super();
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.row = row;
		this.col = col;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getRow() {
		return row;
	}

	public void setRow(int row) {
		this.row = row;
	}

	public int getCol() {
		return col;
	}

	public void setCol(int col) {
		this.col = col;
	}
	
}
