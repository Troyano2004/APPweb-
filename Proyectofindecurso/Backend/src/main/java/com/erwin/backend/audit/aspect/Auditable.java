package com.erwin.backend.audit.aspect;
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {
    String entidad();
    String accion();
    boolean capturarArgs() default false;
}
