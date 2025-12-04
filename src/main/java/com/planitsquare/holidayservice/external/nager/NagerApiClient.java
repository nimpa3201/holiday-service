package com.planitsquare.holidayservice.external.nager;


import com.planitsquare.holidayservice.external.nager.dto.NagerCountryResponse;
import com.planitsquare.holidayservice.external.nager.dto.NagerHolidayResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NagerApiClient {

    private final RestClient nagerRestClient;

    public List<NagerCountryResponse> getAvailableCountries(){
        NagerCountryResponse[] body = nagerRestClient.get()
            .uri("/AvailableCountries")
            .retrieve()
            .body(NagerCountryResponse[].class);

        return body == null ? List.of() : Arrays.asList(body);

    }

    public List<NagerHolidayResponse> getPublicHolidays(int year, String countryCode) {
        NagerHolidayResponse[] body = nagerRestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/PublicHolidays/{year}/{countryCode}")
                .build(year, countryCode))
            .retrieve()
            .body(NagerHolidayResponse[].class);

        return body == null ? List.of() : Arrays.asList(body);
    }

}
