package com.baytalhikmah.sovereign.domain;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = SignupChannelValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SignupChannel {

    String message() default "must be web, mobile, store or agent:<name>";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
