
package FindBackWeb.repository;

import FindBackWeb.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByStatusIgnoreCase(String status);

    List<Item> findByReportTypeIgnoreCase(String reportType);

    List<Item> findByCategoryIgnoreCaseAndStatusIgnoreCase(
            String category, String status);

    List<Item> findByLocationContainingIgnoreCase(String location);

    List<Item> findByItemNameContainingIgnoreCase(String itemName);
}
