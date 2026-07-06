package com.memeboo2.haemi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class HaemiApplicationTests {

	private final DataSource dataSource;

	@Autowired
	HaemiApplicationTests(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Test
	void testProfileUsesIsolatedH2Database() throws SQLException {
		try (var connection = dataSource.getConnection()) {
			assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("H2");
		}
	}

}
