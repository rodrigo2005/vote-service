package com.voteservice.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.voteservice.client.RestClient;
import com.voteservice.converter.VoteConverter;
import com.voteservice.dto.VoteDTO;
import com.voteservice.exception.ClosedSessionException;
import com.voteservice.exception.Messages;
import com.voteservice.exception.UnableToVoteException;
import com.voteservice.model.TopicVoting;
import com.voteservice.model.Vote;
import com.voteservice.repository.VoteRepository;

@RunWith(MockitoJUnitRunner.class)
public class VoteServiceTest {

	@Mock
	private RestClient restClient;
	
	@Mock
	private SessionService sessionService;
	
	@Mock
	private TopicVotingService topicVotingService;
	
	@Mock
	private VoteRepository voteRepository;

	@Mock
	private VoteConverter voteConverter;

	@InjectMocks
	private VoteService voteService;

	
	@Test
	public void shouldReturnAVoteDtoWhenIReceiveAValidVoteDto() {
		final VoteDTO voteDto = new VoteDTO(RandomUtils.nextLong(), RandomStringUtils.randomAlphabetic(11), Boolean.TRUE);
		final Optional<TopicVoting> optionalTopicVoting = Optional.of(new TopicVoting());
		final VoteDTO expected = new VoteDTO(Boolean.TRUE);
		final Vote entity = new Vote();

		when(restClient.validateDocument(voteDto.getDocument())).thenReturn(Boolean.TRUE);
		when(topicVotingService.findById(voteDto.getTopicVotingId())).thenReturn(optionalTopicVoting);
		when(sessionService.isSessionOpenOfTopicVoting(optionalTopicVoting.get())).thenReturn(Boolean.TRUE);
		when(voteConverter.entityFromDto(optionalTopicVoting.get(), voteDto)).thenReturn(entity);
		when(voteRepository.save(entity)).thenReturn(entity);
		when(voteConverter.dtoFromEntity(entity)).thenReturn(expected);
		
		VoteDTO actual = voteService.vote(voteDto);
		assertEquals(expected, actual);
		
		verify(restClient).validateDocument(voteDto.getDocument());
		verify(topicVotingService).findById(voteDto.getTopicVotingId());
		verify(sessionService).isSessionOpenOfTopicVoting(optionalTopicVoting.get());
		verify(voteConverter).entityFromDto(optionalTopicVoting.get(), voteDto);
		verify(voteRepository).save(entity);
		verify(voteConverter).dtoFromEntity(entity);
	}

	@Test
	public void shouldReturnAnExceptionWhenIReceiveATopicVotingThatNotExists() {
		final VoteDTO voteDto = new VoteDTO(RandomUtils.nextLong(), RandomStringUtils.randomAlphabetic(11), Boolean.TRUE);
		final Optional<TopicVoting> optionalTopicVoting = Optional.empty();

		when(restClient.validateDocument(voteDto.getDocument())).thenReturn(Boolean.TRUE);
		when(topicVotingService.findById(voteDto.getTopicVotingId())).thenReturn(optionalTopicVoting);

		assertThatThrownBy(() -> voteService.vote(voteDto))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage(Messages.THE_TOPIC_VOTING_NOT_EXISTS);
		
		verify(restClient).validateDocument(voteDto.getDocument());
		verify(topicVotingService).findById(voteDto.getTopicVotingId());
		verifyZeroInteractions(sessionService);
		verifyZeroInteractions(voteConverter);
		verifyZeroInteractions(voteRepository);
		verifyZeroInteractions(voteConverter);
	}
	
	@Test
	public void shouldReturnAnExceptionWhenIReceiveASessionIsClosed() {
		final VoteDTO voteDto = new VoteDTO(RandomUtils.nextLong(), RandomStringUtils.randomAlphabetic(11), Boolean.TRUE);
		final Optional<TopicVoting> optionalTopicVoting = Optional.of(new TopicVoting());

		when(restClient.validateDocument(voteDto.getDocument())).thenReturn(Boolean.TRUE);
		when(topicVotingService.findById(voteDto.getTopicVotingId())).thenReturn(optionalTopicVoting);
		when(sessionService.isSessionOpenOfTopicVoting(optionalTopicVoting.get())).thenReturn(Boolean.FALSE);

		assertThatThrownBy(() -> voteService.vote(voteDto))
			.isInstanceOf(ClosedSessionException.class)
			.hasMessage(Messages.THE_SESSION_IS_CLOSED);
		
		verify(restClient).validateDocument(voteDto.getDocument());
		verify(topicVotingService).findById(voteDto.getTopicVotingId());
		verify(sessionService).isSessionOpenOfTopicVoting(optionalTopicVoting.get());
		verifyZeroInteractions(voteConverter);
		verifyZeroInteractions(voteRepository);
		verifyZeroInteractions(voteConverter);
	}
	
	@Test
	public void shouldReturnAnExceptionWhenIReceiveADocumentUnableToVote() {
		final VoteDTO voteDto = new VoteDTO(RandomUtils.nextLong(), RandomStringUtils.randomAlphabetic(11), Boolean.TRUE);
		
		when(restClient.validateDocument(voteDto.getDocument())).thenReturn(Boolean.FALSE);
		
		assertThatThrownBy(() -> voteService.vote(voteDto))
			.isInstanceOf(UnableToVoteException.class)
			.hasMessage(Messages.UNABLE_TO_VOTE);
		
		verify(restClient).validateDocument(voteDto.getDocument());
	}

	@Test
	public void shouldReturnAResultOfVoteWhenIReceiveAVoteDtoValid() {
		final VoteDTO voteDto = new VoteDTO(RandomUtils.nextLong());
		final Optional<TopicVoting> optionalTopicVoting = Optional.of(new TopicVoting());
		
		String topicVotingDescription = "Vote of president";
		Long countYes = 10L;
		Long countNo = 5L;
		final VoteDTO expected = new VoteDTO(topicVotingDescription, countYes, countNo);
		
		when(topicVotingService.findById(voteDto.getTopicVotingId())).thenReturn(optionalTopicVoting);
		when(sessionService.isSessionOpenOfTopicVoting(optionalTopicVoting.get())).thenReturn(Boolean.TRUE);
		when(voteRepository.countByTopicVotingAndVoteTrue(optionalTopicVoting.get())).thenReturn(10L);
		when(voteRepository.countByTopicVotingAndVoteFalse(optionalTopicVoting.get())).thenReturn(5L);
		when(voteConverter.dtoFromEntity(optionalTopicVoting.get(), countYes, countNo)).thenReturn(expected);
		
		VoteDTO actual = voteService.result(voteDto);

		assertEquals(expected, actual);
		verify(topicVotingService).findById(voteDto.getTopicVotingId());
		verify(sessionService).isSessionOpenOfTopicVoting(optionalTopicVoting.get());
		verify(voteRepository).countByTopicVotingAndVoteTrue(optionalTopicVoting.get());
		verify(voteRepository).countByTopicVotingAndVoteFalse(optionalTopicVoting.get());
		verify(voteConverter).dtoFromEntity(optionalTopicVoting.get(), countYes, countNo);
	}
	
	@Test
	public void shouldReturnAResultOfVoteWhenIReceiveATopicVotingThatNotExist() {
		final VoteDTO voteDto = new VoteDTO(RandomUtils.nextLong());
		final Optional<TopicVoting> optionalTopicVoting = Optional.empty();
		
		when(topicVotingService.findById(voteDto.getTopicVotingId())).thenReturn(optionalTopicVoting);
		
		assertThatThrownBy(() -> voteService.result(voteDto))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage(Messages.THE_TOPIC_VOTING_NOT_EXISTS);

		verify(topicVotingService).findById(voteDto.getTopicVotingId());
		verifyZeroInteractions(voteRepository);
		verifyZeroInteractions(voteRepository);
		verifyZeroInteractions(voteConverter);
	}
	
	@Test
	public void shouldReturnAResultOfVoteWhenIReceiveATopicVotingSessionIsNotClose() {
		final VoteDTO voteDto = new VoteDTO(RandomUtils.nextLong());
		final Optional<TopicVoting> optionalTopicVoting = Optional.of(new TopicVoting());
		
		when(topicVotingService.findById(voteDto.getTopicVotingId())).thenReturn(optionalTopicVoting);
		when(sessionService.isSessionOpenOfTopicVoting(optionalTopicVoting.get())).thenReturn(Boolean.FALSE);
		
		assertThatThrownBy(() -> voteService.result(voteDto))
			.isInstanceOf(ClosedSessionException.class)
			.hasMessage(Messages.THE_SESSION_IS_NOT_CLOSE);

		verify(topicVotingService).findById(voteDto.getTopicVotingId());
		verify(sessionService).isSessionOpenOfTopicVoting(optionalTopicVoting.get());
		verifyZeroInteractions(voteRepository);
		verifyZeroInteractions(voteRepository);
		verifyZeroInteractions(voteConverter);
	}
	
}
