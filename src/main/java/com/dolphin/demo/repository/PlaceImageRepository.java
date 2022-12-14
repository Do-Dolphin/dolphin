package com.dolphin.demo.repository;

import com.dolphin.demo.domain.Place;
import com.dolphin.demo.domain.PlaceImage;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;


public interface PlaceImageRepository extends JpaRepository<PlaceImage, Long> {
    List<PlaceImage> findAllByPlace(Place place);
    Optional<PlaceImage> findFirstByPlace(Place Place);
    List<PlaceImage> findAllByStateFalse(PageRequest pageRequest);

}
