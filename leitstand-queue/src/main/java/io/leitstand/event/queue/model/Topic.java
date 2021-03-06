/*
 * (c) RtBrick, Inc - All rights reserved, 2015 - 2019
 */
package io.leitstand.event.queue.model;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import io.leitstand.commons.model.AbstractEntity;
import io.leitstand.commons.model.Query;
import io.leitstand.event.queue.jpa.TopicNameConverter;
import io.leitstand.event.queue.service.TopicName;

@Entity
@Table(schema="bus", name="topic")
@NamedQuery(name="Topic.findByName",
			query="SELECT t FROM Topic t WHERE t.topicName=:name")
@NamedQuery(name="Topic.findTopicNames",
			query="SELECT t.topicName FROM Topic t")
@NamedQuery(name="Topic.findAll",
			query="SELECT t FROM Topic t")
public class Topic extends AbstractEntity{

	private static final long serialVersionUID = 1L;


	public static Query<Topic> findTopicByName(TopicName topicName){
		return em -> em.createNamedQuery("Topic.findByName",Topic.class)
					   .setParameter("name", topicName)
					   .getSingleResult();
	}

	public static Query<SortedSet<TopicName>> findTopicNames() {
		return em -> new TreeSet<TopicName>(em.createNamedQuery("Topic.findTopicNames",TopicName.class)
											  .getResultList());
											  
	}

	public static Query<List<Topic>> findTopics() {
		return em -> em.createNamedQuery("Topic.findAll",Topic.class)
						.getResultList();	
	}
	
	
	public Topic() {
		// JPA
	}
	
	public Topic(TopicName topicName) {
		this.topicName = topicName;
	}
	
	@Convert(converter=TopicNameConverter.class)
	@Column(name="name", unique=true)
	private TopicName topicName;

	public TopicName getName() {
		return topicName;
	}
	
}
