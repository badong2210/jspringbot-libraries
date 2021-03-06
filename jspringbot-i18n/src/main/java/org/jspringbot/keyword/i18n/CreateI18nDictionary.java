package org.jspringbot.keyword.i18n;

import org.jspringbot.KeywordInfo;
import org.springframework.stereotype.Component;

@Component
@KeywordInfo(name = "Create I18n Dictionary", description = "Create I18n Dictionary", parameters = {"prefix"})
public class CreateI18nDictionary extends Abstracti18nKeyword {

    @Override
    public Object execute(Object[] params) {
        return i18nHelper.createDictionary(String.valueOf(params[0]));
    }

}
