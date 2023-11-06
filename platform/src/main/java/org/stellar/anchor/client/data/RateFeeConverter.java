package org.stellar.anchor.client.data;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import java.lang.reflect.Type;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import org.stellar.anchor.api.sep.sep38.RateFee;
import org.stellar.anchor.util.GsonUtils;

@Converter
public class RateFeeConverter implements AttributeConverter<RateFee, String> {
  private static final Gson gson = GsonUtils.getInstance();

  @Override
  public String convertToDatabaseColumn(RateFee priceDetails) {
    return gson.toJson(priceDetails);
  }

  @Override
  public RateFee convertToEntityAttribute(String customerInfoJSON) {
    Type type = new TypeToken<RateFee>() {}.getType();
    return gson.fromJson(customerInfoJSON, type);
  }
}
