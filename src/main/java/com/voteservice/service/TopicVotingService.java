package com.voteservice.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.voteservice.converter.TopicVotingConverter;
import com.voteservice.dto.TopicVotingDTO;
import com.voteservice.model.TopicVoting;
import com.voteservice.repository.TopicVotingRepository;

@Service
public class TopicVotingService {

	private TopicVotingRepository topicVotingRepository;
	private TopicVotingConverter topicVotingConverter;

	@Autowired
	public TopicVotingService(TopicVotingRepository topicVotingRepository, TopicVotingConverter topicVotingConverter) {
		this.topicVotingRepository = topicVotingRepository;
		this.topicVotingConverter = topicVotingConverter;
	}
	
	public TopicVotingDTO save(TopicVotingDTO topicVotingDTO) {
		TopicVoting topicVotingToInsert = topicVotingConverter.entityFromDTO(topicVotingDTO);
		TopicVoting topicVotingInserted = topicVotingRepository.save(topicVotingToInsert);
		return topicVotingConverter.dtoFromEntiy(topicVotingInserted);
	}

	public Optional<TopicVoting> findById(Long topicVotingId) {
		return topicVotingRepository.findById(topicVotingId);
	}

}
