package com.zzstack.paas.underlying.metasvr.bean;

import java.util.Map;

import com.zzstack.paas.underlying.utils.FixHeader;
import com.zzstack.paas.underlying.utils.consts.CONSTS;

import io.vertx.core.json.JsonObject;

public class PaasInstance extends BeanMapper {

    private String instId;
    private int cmptId;
    private boolean isDeployed;
    private String status;
    private int posX;
    private int posY;
    private int width;
    private int height;
    private int row;
    private int col;

    public PaasInstance() {
        super();
    }

    /**
     * @param instId
     * @param cmptId
     * @param isDeployed
     * @param posX
     * @param posY
     * @param width
     * @param height
     * @param row
     * @param col
     */
    public PaasInstance(String instId, int cmptId, boolean isDeployed, String status, int posX, int posY, int width, int height,
            int row, int col) {
        super();
        this.instId = instId;
        this.cmptId = cmptId;
        this.isDeployed = isDeployed;
        this.status = status;
        this.posX = posX;
        this.posY = posY;
        this.width = width;
        this.height = height;
        this.row = row;
        this.col = col;
    }

    public static PaasInstance convert(Map<String, Object> mapper) {
        if (mapper == null || mapper.isEmpty())
            return null;

        String instId = getFixDataAsString(mapper, FixHeader.HEADER_INST_ID);
        int cmptId = getFixDataAsInt(mapper, FixHeader.HEADER_CMPT_ID);
        String status = getFixDataAsString(mapper, FixHeader.HEADER_IS_DEPLOYED);
        boolean isDeployed = !status.equals(CONSTS.STR_SAVED); // status.equals(CONSTS.STR_TRUE) || status.equals(CONSTS.STR_WARN);
        int posX = getFixDataAsInt(mapper, FixHeader.HEADER_POS_X);
        int posY = getFixDataAsInt(mapper, FixHeader.HEADER_POS_Y);
        int width = getFixDataAsInt(mapper, FixHeader.HEADER_WIDTH);
        int height = getFixDataAsInt(mapper, FixHeader.HEADER_HEIGHT);
        int row = getFixDataAsInt(mapper, FixHeader.HEADER_ROW);
        int col = getFixDataAsInt(mapper, FixHeader.HEADER_COL);

        return new PaasInstance(instId, cmptId, isDeployed, status, posX, posY, width, height, row, col);
    }

    public String getInstId() {
        return instId;
    }

    public void setInstId(String instId) {
        this.instId = instId;
    }

    public int getCmptId() {
        return cmptId;
    }

    public void setCmptId(int cmptId) {
        this.cmptId = cmptId;
    }

    public boolean isDeployed() {
        return isDeployed;
    }

    public void setDeployed(boolean isDeployed) {
        this.isDeployed = isDeployed;
    }
    
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getPosX() {
        return posX;
    }

    public void setPosX(int posX) {
        this.posX = posX;
    }

    public int getPosY() {
        return posY;
    }

    public void setPosY(int posY) {
        this.posY = posY;
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

    public String toJson() {
        JsonObject retval = new JsonObject();
        retval.put(FixHeader.HEADER_INST_ID, instId);
        retval.put(FixHeader.HEADER_CMPT_ID, cmptId);
        retval.put(FixHeader.HEADER_IS_DEPLOYED, isDeployed);
        retval.put(FixHeader.HEADER_STATUS, status);
        retval.put(FixHeader.HEADER_X, posX);
        retval.put(FixHeader.HEADER_Y, posY);
        retval.put(FixHeader.HEADER_WIDTH, width);
        retval.put(FixHeader.HEADER_HEIGHT, height);
        retval.put(FixHeader.HEADER_ROW, row);
        retval.put(FixHeader.HEADER_COL, col);

        return retval.toString();
    }

    public boolean isDefaultPos() {
        return posX == 0 && posY == 0 && width == CONSTS.POS_DEFAULT_VALUE && height == CONSTS.POS_DEFAULT_VALUE
                && row == CONSTS.POS_DEFAULT_VALUE && col == CONSTS.POS_DEFAULT_VALUE;
    }

    @Override
    public String toString() {
        return "PaasInstance [instId=" + instId + ", cmptId=" + cmptId + ", isDeployed=" + isDeployed + ", status=" + status + ", posX=" + posX
                + ", posY=" + posY + ", width=" + width + ", height=" + height + ", row=" + row + ", col=" + col + "]";
    }

    public JsonObject toJsonObject() {
        JsonObject json = new JsonObject();
        json.put(FixHeader.HEADER_INST_ID, instId);
        json.put(FixHeader.HEADER_CMPT_ID, cmptId);
        json.put(FixHeader.HEADER_IS_DEPLOYED, isDeployed);
        json.put(FixHeader.HEADER_STATUS, status);
        json.put(FixHeader.HEADER_X, posX);
        json.put(FixHeader.HEADER_Y, posY);
        json.put(FixHeader.HEADER_WIDTH, width);
        json.put(FixHeader.HEADER_HEIGHT, height);
        json.put(FixHeader.HEADER_ROW, row);
        json.put(FixHeader.HEADER_COL, col);

        return json;
    }

    public static PaasInstance fromJson(String jsonStr) {
        JsonObject json = new JsonObject(jsonStr);
        String instId = json.getString(FixHeader.HEADER_INST_ID);
        int cmptId = json.getInteger(FixHeader.HEADER_CMPT_ID);
        boolean isDeployed = json.getBoolean(FixHeader.HEADER_IS_DEPLOYED);
        String status = json.getString(FixHeader.HEADER_STATUS);
        int posX = json.getInteger(FixHeader.HEADER_X);
        int posY = json.getInteger(FixHeader.HEADER_Y);
        int width = json.containsKey(FixHeader.HEADER_WIDTH) ? json.getInteger(FixHeader.HEADER_WIDTH) : CONSTS.POS_DEFAULT_VALUE;
        int height = json.containsKey(FixHeader.HEADER_HEIGHT) ? json.getInteger(FixHeader.HEADER_HEIGHT) : CONSTS.POS_DEFAULT_VALUE;
        int row = json.containsKey(FixHeader.HEADER_ROW) ? json.getInteger(FixHeader.HEADER_ROW) : CONSTS.POS_DEFAULT_VALUE;
        int col = json.containsKey(FixHeader.HEADER_COL) ? json.getInteger(FixHeader.HEADER_COL) : CONSTS.POS_DEFAULT_VALUE;

        return new PaasInstance(instId, cmptId, isDeployed, status, posX, posY, width, height, row, col);
    }

}
