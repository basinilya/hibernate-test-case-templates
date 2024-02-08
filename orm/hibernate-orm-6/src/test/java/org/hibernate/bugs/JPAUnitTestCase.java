package org.hibernate.bugs;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.Immutable;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.spi.IntegratorProvider;
import org.hibernate.jpa.boot.spi.JpaSettings;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Persistence;
import jakarta.persistence.Table;

/**
 * This template demonstrates how to develop a test case for Hibernate ORM, using the Java Persistence API.
 */
public class JPAUnitTestCase {

	private static final String CUSTOM_METADATAEXTRACTORINTEGRATOR = "custom.metadataextractorintegrator";

	private static final String LANGUAGE_TABLE_NAME = "LANGUAGE_XXX";

	private static final String B2L_TABLE_NAME = "B2L_XXX";

	private EntityManagerFactory entityManagerFactory;

	@Before
	public void init() {
		final Map<String, Object> configuration = new HashMap<>();

		// <property name="hibernate.integrator_provider"
		// value="org.hibernate.bugs.JPAUnitTestCase.MetadataExtractorIntegratorProvider"
		// />
		configuration.put(JpaSettings.INTEGRATOR_PROVIDER,
				MetadataExtractorIntegratorProvider.class.getName());

		// <property name="hibernate.hbm2ddl.default_constraint_mode"
		// value="NO_CONSTRAINT" />
		configuration.put(AvailableSettings.HBM2DDL_DEFAULT_CONSTRAINT_MODE,
				jakarta.persistence.ConstraintMode.NO_CONSTRAINT.name());

		entityManagerFactory = Persistence.createEntityManagerFactory("templatePU", configuration);
	}

	@After
	public void destroy() {
		if (entityManagerFactory != null) {
			entityManagerFactory.close();
		}
	}

	@Test
	public void hhh123Test() throws Exception {
		final Map<?, ?> foreignKeysSubclass = getForeignKeys(LANGUAGE_TABLE_NAME);

		final Map<?, ?> foreignKeysAssociated = getForeignKeys(B2L_TABLE_NAME);

		Assert.assertEquals("Expected to have no foreign keys in associated table '" + B2L_TABLE_NAME + "'",
				Collections.emptyMap(), foreignKeysAssociated);

		Assert.assertEquals("Expected to have no foreign keys in subclass table '" + LANGUAGE_TABLE_NAME + "'",
				Collections.emptyMap(),
				foreignKeysSubclass);
	}

	private Map<?, ?> getForeignKeys(final String tableName) {
		final org.hibernate.mapping.Table associatedTable = getTable(tableName);
		if (associatedTable != null) {
			return associatedTable.getForeignKeys();
		}
		return null;
	}


	private org.hibernate.mapping.Table getTable(final String tableName) {
		final org.hibernate.mapping.Table res = getTable0(tableName);
		Assert.assertNotNull("Expected to find table '" + tableName + "' in Mapping", res);
		return res;
	}

	private org.hibernate.mapping.Table getTable0(final String tableName) {
		final MetadataExtractorIntegrator metadataExtractorIntegrator = (MetadataExtractorIntegrator) entityManagerFactory
				.unwrap(SessionFactory.class).getProperties().get(CUSTOM_METADATAEXTRACTORINTEGRATOR);
		for (final Namespace namespace : metadataExtractorIntegrator.database.getNamespaces()) {
			for (final org.hibernate.mapping.Table table : namespace.getTables()) {
				if (tableName.equals(table.getName())) {
					return table;
				}
			}
		}
		return null;
	}

	@Entity(name = "VA")
	@Table(name = "VA")
	@Immutable
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class VA {
		public VA() {
			//
		}

		public VA(final long id) {
			this.id = id;
		}

		@Id
		private long id;

		public long getId() {
			return id;
		}

		public void setId(final long id) {
			this.id = id;
		}
	}

	@Entity(name = "Language")
	@Table(name = LANGUAGE_TABLE_NAME)
	@Immutable
	// @jakarta.persistence.PrimaryKeyJoinColumn
	public static class Language extends VA {

		public Language() {
			//
		}

		public Language(final long id, final String code, final String name) {
			super(id);
			this.code = code;
			this.name = name;
		}

		@Basic
		private String code;

		@Basic
		private String name;

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		public String getCode() {
			return code;
		}

		public void setCode(final String code) {
			this.code = code;
		}
	}

	@Entity(name = "Book")
	@Table(name = "BOOK")
	@Immutable
	public static class Book {

		public Book() {
		}

		protected Book(final long id, final String name, final Set<Language> bookLanguages) {
			this.id = id;
			this.name = name;
			this.bookLanguages = bookLanguages;
		}

		@Id
		private long id;

		@Basic
		private String name;

		@ManyToMany
		@JoinTable(name = B2L_TABLE_NAME, joinColumns = @JoinColumn(name = "BOOKID"), inverseJoinColumns = @JoinColumn(name = "LANGID"))
		private Set<Language> bookLanguages;

		public long getId() {
			return id;
		}

		public void setId(final long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		public Set<Language> getBookLanguages() {
			return bookLanguages;
		}

		public void setBookLanguages(final Set<Language> bookLanguages) {
			this.bookLanguages = bookLanguages;
		}
	}

	public static class MetadataExtractorIntegrator implements org.hibernate.integrator.spi.Integrator {

		private Database database;

		public Database getDatabase() {
			return database;
		}

		@Override
		public void integrate(final Metadata metadata, final BootstrapContext bootstrapContext,
				final SessionFactoryImplementor sessionFactory) {
			sessionFactory.getProperties().put(CUSTOM_METADATAEXTRACTORINTEGRATOR, this);
			database = metadata.getDatabase();
		}

		@Override
		public void disintegrate(final SessionFactoryImplementor sessionFactory,
				final SessionFactoryServiceRegistry serviceRegistry) {
		}
	}

	public static class MetadataExtractorIntegratorProvider implements IntegratorProvider {

		@Override
		public List<Integrator> getIntegrators() {
			return Collections.singletonList(new MetadataExtractorIntegrator());
		}
	}

}
