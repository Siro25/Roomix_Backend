package com.team4.rental.repositories;

import com.team4.rental.entities.TenancyEvent;
import com.team4.rental.enums.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface TenancyEventRepository extends JpaRepository<TenancyEvent, Long> {

}
