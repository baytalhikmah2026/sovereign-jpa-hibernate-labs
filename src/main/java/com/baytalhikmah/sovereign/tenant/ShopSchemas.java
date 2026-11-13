package com.baytalhikmah.sovereign.tenant;

import java.util.regex.Pattern;

import org.hibernate.context.spi.TenantSchemaMapper;

/**
 * Maps a tenant to its PostgreSQL schema: the platform uses "public", shop "nile" uses "shop_nile".
 * Anything that is not a well-formed shop code is refused before it can reach the database.
 */
public class ShopSchemas implements TenantSchemaMapper<String> {

    private static final Pattern SHOP_CODE = Pattern.compile("[a-z][a-z0-9]{2,20}");

    @Override
    public String schemaName(String tenant) {
        if (TenantContext.PLATFORM.equals(tenant)) {
            return TenantContext.PLATFORM;
        }
        if (tenant == null || !SHOP_CODE.matcher(tenant).matches()) {
            throw new IllegalArgumentException("Unknown tenant: " + tenant);
        }
        return "shop_" + tenant;
    }

}
