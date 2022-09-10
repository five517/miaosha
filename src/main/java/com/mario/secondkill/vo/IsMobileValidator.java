package com.mario.secondkill.vo;

import com.mario.secondkill.utils.ValidatorUtil;
import com.mario.secondkill.validator.IsMobile;
import org.springframework.util.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class IsMobileValidator implements ConstraintValidator<IsMobile, String> {

    private boolean require = false;
    //初始化获取填值
    @Override
    public void initialize(IsMobile constraintAnnotation) {
        require = constraintAnnotation.require();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if(require) {
            return ValidatorUtil.isMobiile(value);
        } else {
            if(StringUtils.isEmpty(value)) {
                return true;
            } else {
                return ValidatorUtil.isMobiile(value);
            }
        }
    }
}
