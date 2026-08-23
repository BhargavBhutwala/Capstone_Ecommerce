package com.ebookstore.catalog.repository;

import com.ebookstore.catalog.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    List<Product> findByCategoryIdAndActiveTrueAndIdNot(Long categoryId, Long productId);

    List<Product> findByBrandIdAndActiveTrueAndIdNot(Long brandId, Long productId);
}
