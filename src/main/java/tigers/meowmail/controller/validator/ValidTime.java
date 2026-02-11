package tigers.meowmail.controller.validator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TimeValidator.class)
@Documented
public @interface ValidTime {

	String message() default "Invalid time format (HH:mm)";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
