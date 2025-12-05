package com.planitsquare.holidayservice.application.country;

import com.planitsquare.holidayservice.domain.country.Country;
import com.planitsquare.holidayservice.domain.country.CountryRepository;
import com.planitsquare.holidayservice.external.nager.NagerApiClient;
import com.planitsquare.holidayservice.external.nager.dto.NagerCountryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CountrySyncService {

    private final NagerApiClient nagerApiClient;
    private final CountryRepository countryRepository;

    @Transactional
    public void syncCountries() {
        List<NagerCountryResponse> responses = nagerApiClient.getAvailableCountries();

        for (NagerCountryResponse ncr : responses) {

            countryRepository.findByCode(ncr.countryCode())
                .ifPresentOrElse(
                    // 존재하면 갱신
                    existing -> {
                        existing.updateCountry(ncr.countryCode(), ncr.name());
                        existing.markSupported(true);
                    },
                    // 없으면 생성

                    () -> {
                        Country country = new Country(ncr.countryCode(), ncr.name());
                        country.markSupported(true);
                        countryRepository.save(country);

                    }
                );
        }
        log.info("Country 동기화 완료. totalCount={}", countryRepository.count());
    }


}
