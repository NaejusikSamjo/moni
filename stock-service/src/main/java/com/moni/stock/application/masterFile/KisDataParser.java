package com.moni.stock.application.masterFile;

import com.moni.stock.domain.entity.Stock;
import com.moni.stock.domain.entity.Theme;
import com.moni.stock.domain.type.MarketType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Component
public class KisDataParser {

    public List<Stock> parseMasterFile(String url, MarketType market) throws Exception {
        List<Stock> stocks = new ArrayList<>();

        try(ZipInputStream zis = new ZipInputStream(new URL(url).openStream())){
            ZipEntry zipEntry = zis.getNextEntry();

            if(zipEntry != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(zis, "MS949"));
                String line;

                while((line = reader.readLine()) != null) {
                    if (line.length()<228) continue;

                    String rawCode = line.substring(0,9).trim();
                    String ticker = rawCode.length() > 6 ? rawCode.substring(rawCode.length() - 6).trim() : rawCode;

                    int nameEndIndex = line.length() - 228;
                    String name = line.substring(21, nameEndIndex).trim();

                    stocks.add(Stock.builder()
                                    .id(UUID.randomUUID())
                                    .ticker(ticker)
                                    .name(name)
                                    .market(market)
                            .build());
                }
            }
        }
        return stocks;
    }

    public List<Theme> parseThemeMasterFile (String url) throws Exception {
        List<Theme> themes = new ArrayList<>();

        try(ZipInputStream zis = new ZipInputStream(new URL(url).openStream())){
            ZipEntry zipEntry = zis.getNextEntry();

            if(zipEntry != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(zis, "MS949"));
                String line;

                while((line = reader.readLine()) != null) {
                    if(line.length()<=5) continue;

                    String themeCode = line.substring(1,5).trim();
                    String themeName = line.substring(5).trim();

//                    log.info("테마코드 : {}", themeCode);
//                    log.info("테마이름 : {}", themeName);

                    themes.add(Theme.builder()
                                    .id(UUID.randomUUID())
                                    .themeCode(themeCode)
                                    .themeName(themeName)
                            .build());
                }
            }
        }
        return themes;
    }
}
