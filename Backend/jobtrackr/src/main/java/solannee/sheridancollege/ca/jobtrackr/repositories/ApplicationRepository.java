package solannee.sheridancollege.ca.jobtrackr.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import solannee.sheridancollege.ca.jobtrackr.beans.Application;


@Repository
public interface ApplicationRepository
        extends JpaRepository<Application, Long> {

}