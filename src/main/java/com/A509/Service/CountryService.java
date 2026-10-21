package com.A509.Service;

import com.A509.Entity.Country;
import com.A509.Repository.CountryRepository;
import com.A509.Repository.UniformCategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CountryService {

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private UniformCategoryRepository categoryRepository;

    public List<Country> getAllCountries() {
        return countryRepository.findAll();
    }

    public Optional<Country> getCountryById(Long id) {
        return countryRepository.findById(id);
    }

    public Country addCountry(Country country) {
        if (countryRepository.existsByCountryName(country.getCountryName())) {
            throw new RuntimeException("Quốc gia đã tồn tại!");
        }
        return countryRepository.save(country);
    }

    public Country updateCountry(Long id, Country countryDetails) {
        Country existingCountry = countryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Quốc gia không tồn tại"));

        if (!existingCountry.getCountryName().equals(countryDetails.getCountryName())
                && countryRepository.existsByCountryName(countryDetails.getCountryName())) {
            throw new RuntimeException("Tên quốc gia đã được sử dụng!");
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
            throw new RuntimeException("Không thể xóa quốc gia này vì đã có dữ liệu quân phục liên quan.");
        }

        if (categoryRepository.existsByCountryId(id)) {
            throw new RuntimeException("Không thể xóa quốc gia này vì đã có nội dung phục dựng liên quan.");
        }

        countryRepository.delete(country);
    }
}
