package com.moneybook.backend.holiday.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Reads the KASI special day XML API; only public holidays enter the cache. */
@Component
public class KasiHolidayApiClient implements HolidayApiClient {
    private static final int PAGE_SIZE = 100;
    private final RestClient restClient;
    private final String serviceKey;

    public KasiHolidayApiClient(
            @Value("${HOLIDAY_API_URL:https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo}")
            String endpoint,
            @Value("${HOLIDAY_API_SERVICE_KEY:}") String serviceKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.restClient = RestClient.builder().baseUrl(endpoint).requestFactory(factory).build();
        this.serviceKey = serviceKey;
    }

    @Override
    public boolean configured() {
        return !serviceKey.isBlank();
    }

    @Override
    public List<HolidayData> fetchYear(int year) {
        if (!configured()) throw new HolidayApiException("Holiday API key is missing");
        List<HolidayData> result = new ArrayList<>();
        int totalCount;
        int page = 1;
        do {
            Page response = fetchPage(year, page);
            result.addAll(response.holidays());
            totalCount = response.totalCount();
            page++;
        } while ((page - 1) * PAGE_SIZE < totalCount && page <= 10);
        if ((page - 1) * PAGE_SIZE < totalCount) throw new HolidayApiException("Holiday API pagination limit exceeded");
        return result;
    }

    private Page fetchPage(int year, int page) {
        try {
            String xml = restClient.get().uri(builder -> builder
                            .queryParam("ServiceKey", serviceKey)
                            .queryParam("solYear", year)
                            .queryParam("numOfRows", PAGE_SIZE)
                            .queryParam("pageNo", page).build())
                    .retrieve().body(String.class);
            return parse(xml, year);
        } catch (HolidayApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new HolidayApiException("Holiday API request failed", exception);
        }
    }

    /** Validates the complete API envelope before accepting any row for cache replacement. */
    Page parse(String xml, int year) {
        if (xml == null || xml.isBlank()) throw new HolidayApiException("Empty holiday API response");
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            Element root = document.getDocumentElement();
            if (!"response".equals(root.getTagName()) || !"00".equals(text(root, "resultCode"))) {
                throw new HolidayApiException("Holiday API returned an error");
            }
            int total = Integer.parseInt(text(root, "totalCount"));
            if (total < 0 || total > 1000) throw new HolidayApiException("Invalid holiday count");
            NodeList items = root.getElementsByTagName("item");
            if (total > 0 && items.getLength() == 0) throw new HolidayApiException("Missing holiday items");
            List<HolidayData> holidays = new ArrayList<>();
            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                String flag = text(item, "isHoliday");
                if (!"Y".equals(flag) && !"N".equals(flag)) throw new HolidayApiException("Invalid holiday flag");
                LocalDate date = LocalDate.parse(text(item, "locdate"), DateTimeFormatter.BASIC_ISO_DATE);
                if (date.getYear() != year) throw new HolidayApiException("Holiday date outside requested year");
                String name = text(item, "dateName");
                String type = text(item, "dateKind");
                if (name.isBlank() || name.length() > 100 || type.length() > 30) {
                    throw new HolidayApiException("Invalid holiday item");
                }
                if ("Y".equals(flag)) holidays.add(new HolidayData(date, name, type));
            }
            return new Page(total, holidays);
        } catch (HolidayApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new HolidayApiException("Invalid holiday API XML", exception);
        }
    }

    private String text(Element element, String tag) {
        NodeList children = element.getElementsByTagName(tag);
        return children.getLength() == 0 ? "" : children.item(0).getTextContent().trim();
    }

    record Page(int totalCount, List<HolidayData> holidays) { }
}
