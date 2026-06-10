package pl.pb.tc.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import pl.pb.tc.dto.RegistrationForm;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, RegistrationForm> {

    @Override
    public boolean isValid(RegistrationForm form, ConstraintValidatorContext context) {
        if (form.getPassword() == null) {
            return true;
        }
        boolean matches = form.getPassword().equals(form.getConfirmPassword());
        if (!matches) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                    .addPropertyNode("confirmPassword")
                    .addConstraintViolation();
        }
        return matches;
    }
}
