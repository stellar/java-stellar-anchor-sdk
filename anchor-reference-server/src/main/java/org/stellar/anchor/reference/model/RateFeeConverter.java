package org.stellar.anchor.reference.model;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.lang.reflect.Type;
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
