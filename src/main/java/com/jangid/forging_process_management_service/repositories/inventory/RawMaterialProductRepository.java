package com.jangid.forging_process_management_service.repositories.inventory;

import com.jangid.forging_process_management_service.entities.inventory.RawMaterialProduct;
import com.jangid.forging_process_management_service.entities.product.Product;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RawMaterialProductRepository extends CrudRepository<RawMaterialProduct, Long> {

  List<RawMaterialProduct> findByProductAndDeletedFalse(Product product);

}
