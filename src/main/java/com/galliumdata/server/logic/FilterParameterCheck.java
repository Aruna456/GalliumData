// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.logic;

import java.util.Iterator;
import com.galliumdata.server.repository.RepositoryException;
import com.galliumdata.server.repository.FilterUse;

public class FilterParameterCheck
{
    public static void verifyParameters(final FilterParameterDefinition def, final FilterUse use) {
        for (final FilterParameter param : def.getParameterDefinitions().values()) {
            Object value = use.getParameters().get(param.name);
            if (value == null && param.required) {
                throw new RepositoryException("repo.MissingProperty", new Object[] { param.name });
            }
            if (value == null && param.defaultValue != null && param.defaultValue.trim().length() > 0) {
                if (param.type == FilterParameter.ParameterType.BOOLEAN) {
                    value = Boolean.valueOf(param.defaultValue);
                }
                else if (param.type == FilterParameter.ParameterType.INTEGER) {
                    value = Integer.valueOf(param.defaultValue);
                }
                else {
                    if (param.type != FilterParameter.ParameterType.STRING) {
                        throw new RuntimeException("Unexpected condition: filter parameter has unknown type: " + String.valueOf(param.type));
                    }
                    value = param.defaultValue;
                }
            }
            if (value == null) {
                continue;
            }
            if (param.type == FilterParameter.ParameterType.BOOLEAN && !(value instanceof Boolean)) {
                throw new RepositoryException("repo.InvalidPropertyValue", new Object[] { param.name, value, use.getName() });
            }
            if (param.type == FilterParameter.ParameterType.STRING && !(value instanceof String)) {
                throw new RepositoryException("repo.InvalidPropertyValue", new Object[] { param.name, value, use.getName() });
            }
            if (param.type == FilterParameter.ParameterType.INTEGER && !(value instanceof Integer)) {
                throw new RepositoryException("repo.InvalidPropertyValue", new Object[] { param.name, value, use.getName() });
            }
        }
    }
}
