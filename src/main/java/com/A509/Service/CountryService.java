package com.A509.Service;

import com.A509.Entity.Country;
import com.A509.Repository.CountryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CountryService {

    @Autowired
    private CountryRepository countryRepository;

    public List<Country> getAllCountries() {
        return countryRepository.findAll();
    }

    public Optional<Country> getCountryById(Long id) {
        return countryRepository.findById(id);
    }

    public Country addCountry(Country country) {
        if (countryRepository.existsByCountryName(country.getCountryName())) {
            throw new RuntimeException("Quốc gia '" + country.getCountryName() + "' đã tồn tại!");
        }
        return countryRepository.save(country);
    }

    public Country updateCountry(Long id, Country countryDetails) {
        Country existingCountry = countryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quốc gia không tồn tại"));

        if (!existingCountry.getCountryName().equals(countryDetails.getCountryName())) {
            if (countryRepository.existsByCountryName(countryDetails.getCountryName())) {
                throw new RuntimeException("Tên quốc gia '" + countryDetails.getCountryName() + "' đã được sử dụng!");
            }
        }

        existingCountry.setCountryName(countryDetails.getCountryName());
        existingCountry.setContinent(countryDetails.getContinent());
        existingCountry.setFlagImageUrl(countryDetails.getFlagImageUrl());
        existingCountry.setDescription(countryDetails.getDescription());

        return countryRepository.save(existingCountry);
    }

    public List<Country> searchCountries(String keyword) {
        return countryRepository.findByCountryNameContainingIgnoreCase(keyword);
    }

    @Transactional
    public void deleteCountry(Long id) {
        Country country = countryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quốc gia không tồn tại"));

        if (country.getUniforms() != null && !country.getUniforms().isEmpty()) {
            throw new RuntimeException("Không thể xóa quốc gia này vì đã có dữ liệu Quân phục liên quan.");
        }

        countryRepository.delete(country);
    }
}
