package com.google.gcs.sdrs.dao.impl;

import java.io.Serializable;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import com.google.gcs.sdrs.dao.DAO;

/**
 * Hibernate based Generic DAO implementation
 *
 * @param <T>
 * @param <Id>
 */
public class GenericDAO<T, Id extends Serializable> implements DAO<T, Id> {

  private final Class<T> type;

  private static StandardServiceRegistry registry;
  private static SessionFactory sessionFactory;

  protected Session currentSession;
  protected Transaction currentTransaction;

  public GenericDAO(final Class<T> type) {
    this.type = type;
  }

  /* (non-Javadoc)
   * @see com.google.gcs.sdrs.dao.impl.DAO#persist(T)
   */
  @Override
  public void persist(final T entity) {
    openCurrentSessionWithTransaction();
    getCurrentSession().save(entity);
    closeCurrentSessionwithTransaction();
  }

  /* (non-Javadoc)
   * @see com.google.gcs.sdrs.dao.impl.DAO#update(T)
   */
  @Override
  public void update(final T object) {
    openCurrentSessionWithTransaction();
    getCurrentSession().update(object);
    closeCurrentSessionwithTransaction();
  }

  /* (non-Javadoc)
   * @see com.google.gcs.sdrs.dao.impl.DAO#findById(Id)
   */
  @Override
  @SuppressWarnings("unchecked")
  public T findById(Id id) {
    openCurrentSession(); // no transaction per se for a find
    Object object = getCurrentSession().get(type, id);
    closeCurrentSession();
    return (T) object;
  }

  /* (non-Javadoc)
   * @see com.google.gcs.sdrs.dao.impl.DAO#delete(T)
   */
  @Override
  public void delete(final T object) {
    openCurrentSessionWithTransaction();
    getCurrentSession().delete(object);
    closeCurrentSessionwithTransaction();
  }

  public Session openCurrentSession() {
    currentSession = getSessionFactory().openSession();
    return currentSession;
  }

  public Session openCurrentSessionWithTransaction() {
    currentSession = getSessionFactory().openSession();
    currentTransaction = currentSession.beginTransaction();
    return currentSession;
  }

  public void closeCurrentSession() {
    currentSession.close();
  }

  public void closeCurrentSessionwithTransaction() {
    currentTransaction.commit();
    currentSession.close();
  }

  public static SessionFactory getSessionFactory() {
    if (sessionFactory == null) {
      try {
        // Create registry
        registry = new StandardServiceRegistryBuilder().configure().build();

        // Create MetadataSources
        MetadataSources sources = new MetadataSources(registry);

        // Create Metadata
        Metadata metadata = sources.getMetadataBuilder().build();

        // Create SessionFactory
        sessionFactory = metadata.getSessionFactoryBuilder().build();

      } catch (Exception e) {
        e.printStackTrace();
        if (registry != null) {
          StandardServiceRegistryBuilder.destroy(registry);
        }
      }
    }
    return sessionFactory;
  }

  /*
  private static SessionFactory getSessionFactory() {
		Configuration configuration = new Configuration().configure();
		StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder()
				.applySettings(configuration.getProperties());
		SessionFactory sessionFactory = configuration.buildSessionFactory(builder.build());
		return sessionFactory;

		Configuration config = new Configuration().configure();
		ServiceRegistry servReg = new StandardServiceRegistryBuilder().applySettings(config.getProperties()).build();
		SessionFactory factory = config.buildSessionFactory(servReg);
		return factory;

	}
  */

  public Session getCurrentSession() {
    return currentSession;
  }

  public void setCurrentSession(Session currentSession) {
    this.currentSession = currentSession;
  }

  public Transaction getCurrentTransaction() {
    return currentTransaction;
  }

  public void setCurrentTransaction(Transaction currentTransaction) {
    this.currentTransaction = currentTransaction;
  }
}
