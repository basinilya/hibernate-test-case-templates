package org.hibernate.bugs;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;

/**
 * This template demonstrates how to develop a standalone test case for Hibernate ORM.  Although this is perfectly
 * acceptable as a reproducer, usage of ORMUnitTestCase is preferred!
 */
public class TestSelectDistinctDiscriminator {

	private static final String TABLE_NAME_COUNTRY = "COUNTRY";
	private static final String TABLE_NAME_LANGUAGE = "LANGUAGE";

	private SessionFactory sf;
	private Session session;
	private long nextId;

	@Before
	public void setup() {
		final StandardServiceRegistryBuilder srb = new StandardServiceRegistryBuilder()
			// Add in any settings that are specific to your test. See resources/hibernate.properties for the defaults.
			.applySetting( "hibernate.show_sql", "true" )
			.applySetting( "hibernate.format_sql", "true" )
			.applySetting( "hibernate.hbm2ddl.auto", "update" );

		final Metadata metadata = new MetadataSources( srb.build() )
		// Add your entities here.
				.addAnnotatedClass(VA.class) //
				.addAnnotatedClass(Country.class) //
				.addAnnotatedClass(Language.class) //
			.buildMetadata();

		sf = metadata.buildSessionFactory();
		session = sf.openSession();

		// drop tables that MUST NOT be used in SQL
		session.getTransaction().begin();
		session.createNativeQuery("drop table " + TABLE_NAME_COUNTRY, Integer.class).executeUpdate();
		session.getTransaction().commit();
		session.getTransaction().begin();
		session.createNativeQuery("drop table " + TABLE_NAME_LANGUAGE, Integer.class).executeUpdate();
		session.getTransaction().commit();
	}

	@After
	public void destroy() {
		if (sf != null) {
			sf.close();
		}
	}

	// Add your tests, using standard JUnit.

	@Test
	public void hhh123TestSelectBasic() throws Exception {
		final TypedQuery<String> query = session.createQuery("select distinct a.code from VA a", String.class);
		query.getResultList();
	}

	@Test
	public void hhh123TestSelectClass() throws Exception {
		final TypedQuery<String> query = session.createQuery("select distinct a.class from VA a", String.class);
		query.getResultList();
	}

	@Test
	public void hhh123TestSelectType() throws Exception {
		final TypedQuery<String> query = session.createQuery("select distinct type(a) from VA a", String.class);
		query.getResultList();
	}

	long getNextId() {
		nextId++;
		return nextId;
	}

	@Entity(name = "VA")
	@Inheritance(strategy = InheritanceType.JOINED)
	@DiscriminatorColumn(discriminatorType = DiscriminatorType.STRING)
	public static class VA {

		@Basic
		private String code;

		@Basic
		private String name;

		public VA() {
			//
		}

		public VA(final long id, final String code, final String name) {
			this.id = id;
			this.code = code;
			this.name = name;
		}

		@Id
		private long id;

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

		public String getCode() {
			return code;
		}

		public void setCode(final String code) {
			this.code = code;
		}
	}

	@Entity(name = "Country")
	@Table(name = TABLE_NAME_COUNTRY)
	public static class Country extends VA {

		public Country() {
			//
		}

		public Country(final long id, final String code, final String name) {
			super(id, code, name);
		}

	}

	@Entity(name = "Language")
	@Table(name = TABLE_NAME_LANGUAGE)
	public static class Language extends VA {

		public Language() {
			//
		}

		public Language(final long id, final String code, final String name) {
			super(id, code, name);
		}

	}

}
