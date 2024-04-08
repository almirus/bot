package com.almirus.kvartalyBot.util;

import com.almirus.kvartalyBot.dal.entity.Environment;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;

public class Weather {
    public static <T> T get(URL url, Class<T> type) throws IOException {
        ObjectMapper mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
        mapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);

        return mapper.readValue(url, type);
    }
    @NotNull
    public static String getUvStr(Environment weather) {
        String uvStr = "темно или сильная облачность";
        if (weather.getUvIndex() > 0 && weather.getUvIndex() <= 3) uvStr = "низкий";
        else if (weather.getUvIndex() > 3 && weather.getUvIndex() <= 6) uvStr = "средний";
        else if (weather.getUvIndex() > 6 && weather.getUvIndex() <= 8) uvStr = "высокий";
        else if (weather.getUvIndex() > 8 && weather.getUvIndex() <= 11) uvStr = "очень высокий";
        else if (weather.getUvIndex() > 11) uvStr = "экстремальный";
        return uvStr;
    }
    @NotNull
    public static String getRainStr(Environment weather) {
        String rainStr = "дождя нет";
        if (weather.getRainCount() > 0 && weather.getRainCount() <= 50) rainStr = "слабый дождь";
        else if (weather.getRainCount() > 50 && weather.getRainCount() <= 120) rainStr = "сильный дождь";
        else if (weather.getRainCount() > 120) rainStr = "очень сильный дождь";
        return rainStr;
    }
}
