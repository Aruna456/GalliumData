// 
// Decompiled by Procyon v0.6.0
// 

package com.galliumdata.server.repository;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface Persisted {
    String JSONName() default "";
    
    String[] allowedValues() default {};
    
    String fileName() default "";
    
    String directoryName() default "";
    
    Class<? extends RepositoryObject> memberClass() default NullClass.class;
}
