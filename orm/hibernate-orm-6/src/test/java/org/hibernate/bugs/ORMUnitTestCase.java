/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.bugs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.annotations.JdbcType;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.type.descriptor.jdbc.LongVarcharJdbcType;
import org.junit.Test;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;

/**
 * This template demonstrates how to develop a test case for Hibernate ORM, using its built-in unit test framework.
 * Although ORMStandaloneTestCase is perfectly acceptable as a reproducer, usage of this class is much preferred.
 * Since we nearly always include a regression test with bug fixes, providing your reproducer using this method
 * simplifies the process.
 *
 * What's even better?  Fork hibernate-orm itself, add your test case directly to a module's unit tests, then
 * submit it as a PR!
 */
public class ORMUnitTestCase extends BaseCoreFunctionalTestCase {

	// Add your entities here.
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				RootEntity.class, EntryEntity.class
		};
	}

	// If you use *.hbm.xml mappings, instead of annotations, add the mappings here.
	@Override
	protected String[] getMappings() {
		return new String[] {
//				"Foo.hbm.xml",
//				"Bar.hbm.xml"
		};
	}
	// If those mappings reside somewhere other than resources/org/hibernate/test, change this.
	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/test/";
	}

	// Add in any settings that are specific to your test.  See resources/hibernate.properties for the defaults.
	@Override
	protected void configure(final Configuration configuration) {
		super.configure( configuration );

		configuration.setProperty( AvailableSettings.SHOW_SQL, Boolean.TRUE.toString() );
		configuration.setProperty( AvailableSettings.FORMAT_SQL, Boolean.TRUE.toString() );
		//configuration.setProperty( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	// Add your tests, using standard JUnit.
	@Test
	public void hhh18943Test() throws Exception {
		// BaseCoreFunctionalTestCase automatically creates the SessionFactory and provides the Session.
		final Session s = openSession();
		final Transaction tx = s.beginTransaction();
		// Do stuff...
		
		final RootEntity root = new RootEntity();
		root.rootId = UUID.randomUUID();
		root.entries = new ArrayList<>();

		for (int i = 0; i < 2; i++) {
			final EntryEntity entry = new EntryEntity();
			final ChildId entryId = new ChildId();
			entryId.rootId = root.rootId;
			entryId.position = (long) i;
			entry.entryId = entryId;
			entry.root = root;
			entry.entryName = "some" + i;
			root.entries.add(entry);
		}
		
		s.persist(root);
		for (final EntryEntity entry : root.entries) {
			s.persist(entry);
		}
		
		tx.commit();
		s.close();
	}

	@Entity
	private static class RootEntity {

		private static final String ROOT_ID = "ROOT_ID";

		@Id
		@Column(name = RootEntity.ROOT_ID)
		private UUID rootId;

		@OneToMany(mappedBy = "root", cascade = CascadeType.REMOVE)
		// In 6.1.7 @OrderColumn causes redundant update after insert when
		// list position is correct and unique violation if list position is wrong
		// In 6.6.3 it causes NPE, see
		// https://discourse.hibernate.org/t/hibernate-6-upgrade-causes-nullpointerexception/8780
		@OrderColumn(name = ChildId.POSITION, updatable = false)
		@OrderBy("entryId.position")
		private List<EntryEntity> entries;

	}

	@Entity
	private static class EntryEntity {
		@EmbeddedId
		private ChildId entryId;

		@ManyToOne
		@MapsId("rootId")
		@JoinColumn(name = RootEntity.ROOT_ID)
		private RootEntity root;

		@JdbcType(LongVarcharJdbcType.class)
		@Basic(optional = false)
		private String entryName;
	}

	@Embeddable
	private static class ChildId implements Serializable {

		private static final long serialVersionUID = 1L;

		public static final String POSITION = "POSITION";

		// Column name is overridden by implicit @JoinColumn in @MapsId
		// @Column(name = ROOT_ID)
		private UUID rootId;

		@Column(name = POSITION)
		private Long position;

		@Override
		public int hashCode() {
			return Objects.hash(rootId, position);
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final ChildId other = (ChildId) obj;
			return Objects.equals(rootId, other.rootId) && Objects.equals(position, other.position);
		}

	}

}
