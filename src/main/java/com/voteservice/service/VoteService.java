package com.voteservice.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.voteservice.client.RestClient;
import com.voteservice.converter.VoteConverter;
import com.voteservice.dto.VoteDTO;
import com.voteservice.exception.ClosedSessionException;
import com.voteservice.exception.Messages;
import com.voteservice.exception.UnableToVoteException;
import com.voteservice.model.TopicVoting;
import com.voteservice.model.Vote;
import com.voteservice.repository.VoteRepository;

@Service
public class VoteService {

	private TopicVotingService topicVotingService;
	private SessionService sessionService;
	private VoteConverter voteConverter;
	private VoteRepository voteRepository;
	private RestClient restClient;

	@Autowired
	public VoteService(TopicVotingService topicVotingService, SessionService sessionService, VoteConverter voteConverter, VoteRepository voteRepository, RestClient restClient) {
		this.topicVotingService = topicVotingService;
		this.sessionService = sessionService;
		this.voteConverter = voteConverter;
		this.voteRepository = voteRepository;
		this.restClient = restClient;
	}
	
	public VoteDTO vote(VoteDTO voteDto) {
		if (restClient.validateDocument(voteDto.getDocument())) {
			Optional<TopicVoting> optionalTopicVoting = topicVotingService.findById(voteDto.getTopicVotingId());
			if (optionalTopicVoting.isPresent()) {
				if (sessionService.isSessionOpenOfTopicVoting(optionalTopicVoting.get())) {
					return executeVote(voteDto, optionalTopicVoting);
				}
				throw new ClosedSessionException(Messages.THE_SESSION_IS_CLOSED);
			}
			throw new IllegalArgumentException(Messages.THE_TOPIC_VOTING_NOT_EXISTS);
		}
		throw new UnableToVoteException(Messages.UNABLE_TO_VOTE);
	}

	private VoteDTO executeVote(VoteDTO voteDto, Optional<TopicVoting> optionalTopicVoting) {
		Vote vote = voteConverter.entityFromDto(optionalTopicVoting.get(), voteDto);
		Vote voteSaved = voteRepository.save(vote);
		return voteConverter.dtoFromEntity(voteSaved);
	}

	public VoteDTO result(VoteDTO voteDto) {
		Optional<TopicVoting> optionalTopicVoting = topicVotingService.findById(voteDto.getTopicVotingId());
		if (optionalTopicVoting.isPresent()) {
			if (sessionService.isSessionOpenOfTopicVoting(optionalTopicVoting.get())) {
				Long countYes = voteRepository.countByTopicVotingAndVoteTrue(optionalTopicVoting.get());
				Long countNo = voteRepository.countByTopicVotingAndVoteFalse(optionalTopicVoting.get());
				return voteConverter.dtoFromEntity(optionalTopicVoting.get(), countYes, countNo);
			}
			throw new ClosedSessionException(Messages.THE_SESSION_IS_NOT_CLOSE);
		}
		throw new IllegalArgumentException(Messages.THE_TOPIC_VOTING_NOT_EXISTS);
	}

}
