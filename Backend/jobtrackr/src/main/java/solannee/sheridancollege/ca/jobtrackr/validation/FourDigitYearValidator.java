package solannee.sheridancollege.ca.jobtrackr.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class FourDigitYearValidator implements ConstraintValidator<FourDigitYear, LocalDate> {
    private static final int MINIMUM_FOUR_DIGIT_YEAR = 1000;
    private static final int MAXIMUM_FOUR_DIGIT_YEAR = 9999;

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        return value == null
                || (value.getYear() >= MINIMUM_FOUR_DIGIT_YEAR
                    && value.getYear() <= MAXIMUM_FOUR_DIGIT_YEAR);
    }
}
