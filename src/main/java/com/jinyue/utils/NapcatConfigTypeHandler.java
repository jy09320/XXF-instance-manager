package com.jinyue.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinyue.dto.NapcatConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
public class NapcatConfigTypeHandler extends BaseTypeHandler<NapcatConfig> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, NapcatConfig parameter, JdbcType jdbcType) throws SQLException {
        try {
            PGobject jsonObject = new PGobject();
            jsonObject.setType("jsonb");
            jsonObject.setValue(objectMapper.writeValueAsString(parameter));
            ps.setObject(i, jsonObject);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert NapcatConfig to JSONB", e);
            throw new SQLException("Failed to convert NapcatConfig to JSONB", e);
        }
    }

    @Override
    public NapcatConfig getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return parseJson(json);
    }

    @Override
    public NapcatConfig getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return parseJson(json);
    }

    @Override
    public NapcatConfig getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return parseJson(json);
    }

    private NapcatConfig parseJson(String json) throws SQLException {
        if (json == null || json.trim().isEmpty()) {
            return new NapcatConfig(); // 返回默认配置
        }
        try {
            return objectMapper.readValue(json, NapcatConfig.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSONB to NapcatConfig: {}", json, e);
            throw new SQLException("Failed to parse JSONB to NapcatConfig", e);
        }
    }
}