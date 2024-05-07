package com.example.sprint1.service;
import com.example.sprint1.dto.CountFollowersUserDto;

import com.example.sprint1.exception.BadRequestException;
import com.example.sprint1.dto.FollowerListDto;
import com.example.sprint1.dto.FollowerUsersDto;
import com.example.sprint1.dto.FollowListDto;
import com.example.sprint1.dto.FollowdUserDto;
import com.example.sprint1.exception.NotFoundException;
import com.example.sprint1.model.User;
import com.example.sprint1.repository.IUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

@Service
public class UserServiceImpl implements IUserService{

    @Autowired
    IUserRepository userRepository;

    /**
    * Adds a follower to the set of follower and a followed to the set of followed
    * @param userID Id of the user who will follow
     * @param userIdToFollow Id of the user to follow
     */
    @Override
    public void addFollower(Integer userID, Integer userIdToFollow) {
        //Create optional of a possible user
        Optional<User> userAuxOptional = Optional.ofNullable(userRepository.findUserById(userID));
        //Create optional of a possible user To Follow
        Optional<User> userToFollowOptional = Optional.ofNullable(userRepository.findUserById(userIdToFollow));
        //Lambda
        userAuxOptional.ifPresentOrElse(
                //We ask if user to follow exists
                userAux -> userToFollowOptional.ifPresentOrElse(
                        userToFollow -> {
                            //We ask if user is trying to follow himself
                            if (userID.equals(userIdToFollow)) {
                                throw new BadRequestException("Can't follow yourself");
                            }
                            //We check if user is already followed
                            if (userAux.getFollowed().contains(userToFollow.getId()) || userToFollow.getFollowers().contains(userAux.getId())) {
                                throw new BadRequestException("User already followed");
                            }
                            //None of the cases creates conflict, we simply update the followers and followed
                            userRepository.updateUserFollower(userAux, userToFollow);
                        },
                        () -> {
                            throw new BadRequestException("User to follow not found");
                        }
                ),
                () -> {
                    throw new BadRequestException("User not found");
                }
        );
    }

    /**
     * Method to retrieve the follower count for a given user. REQ US0002
     * @param userId
     * @return
     */
    @Override
    public CountFollowersUserDto getFollowerCount(Integer userId) {
        // Retrieve all users from the repository.
        List<User> userList = userRepository.findAll();

        // Initialize variables to store follower count and username.
        Integer followerCount = 0;
        String name = "";

        // Validate in all users if the user is followed
        for (User u : userList) {
            // Check if the current user matches the given userId.
            if(u.getId().equals(userId)){
                // If matched, store the username.
                name = u.getUser_name();
            }
            // Check if the current user's followers list contains the given userId.
            if (u.getFollowers().contains(userId)) {
                // If yes, increment the follower count.
                followerCount++;
            }
        }

        // If no user is found with the given userId, throw a NotFoundException.
        if(name==""){
            throw new NotFoundException("No se encontró al vendedor");
        }

        // Return a CountFollowersUserDto object containing userId, username, and follower count.
        return new CountFollowersUserDto(userId,name,followerCount);
    }


    /**
     * Get the list of followers for a user
     * @param userId The ID of the user
     * @param order The order in which to return the followers
     * @return The list of followers for the user
     */
    @Override
    public FollowerListDto getFollowerList(Integer userId, String order) {
        // Get the user by ID and check if the user exists
        Optional<User> optionalUser = userRepository.getUserById(userId);
        User principalUser = optionalUser.orElseThrow(
                () -> new NotFoundException("No se encontró el usuario con el ID proporcionado"));

        List<FollowerUsersDto> followersList = new ArrayList();

        Set<Integer> followers = principalUser.getFollowers();

        // Iterate over the followers and add them to the list in DTO format
        for (Integer miniId : followers) {
            optionalUser = userRepository.getUserById(miniId);
            optionalUser.ifPresent(user -> followersList.add(convertToFollowUserDto(user)));
        }
        return new FollowerListDto(principalUser.getId(), principalUser.getUser_name(), followersList);
    }

    /**
     * Get all the followers from user using followerListDto
     * @param userId
     * @return
     */
    public FollowerListDto getFollowerList(Integer userId) {
        List<User> allUsers = userRepository.findAll();
        Optional<User> userSpecified = allUsers.stream().filter(user -> user.getId() == userId).findFirst();
        if (userSpecified.isPresent() && userSpecified.get().getFollowers()!=null){
            Set<Integer> followerList = userSpecified.get().getFollowers();
            List<FollowerUsersDto> followerUsersDto =  allUsers.stream()
                    .filter(user ->  followerList.contains(user.getId()))
                    .map(user -> new FollowerUsersDto(user.getId(), user.getUser_name())).toList();
            FollowerListDto followerListDto = new FollowerListDto();
            followerListDto.setFollowers(followerUsersDto);
            followerListDto.setUser_id(userId);
            followerListDto.setUser_name(userSpecified.get().getUser_name());
            return followerListDto;
        }
        else {
            throw new NotFoundException("User not Found");
        }

    }

    /**
     * Gets the followed list
     * @param userId
     * @return
     */
    @Override
    public  FollowListDto getFollowedList(Integer userId) {
        // Get all users from repository
        List<User> allUsers = userRepository.findAll();
        // Select the user that matches with the id supplied
        Optional<User> userSpecified = allUsers.stream().filter(user -> user.getId() == userId).findFirst();
        if(userSpecified.isPresent()&&userSpecified.get().getFollowed()!=null){
            // Get the list of users that the user follows
            Set<Integer> followedList = userSpecified.get().getFollowed();
            // Return the list of users that the user follows pa
            List<FollowdUserDto> followdUsersDtos = allUsers.stream()
                    .filter(user -> followedList.contains(user.getId()))
                    .map(user -> new FollowdUserDto(user.getId(), user.getUser_name())).toList();
            FollowListDto followedListDto = new FollowListDto();
            followedListDto.setFollowed(followdUsersDtos);
            followedListDto.setUser_id(userId);
            followedListDto.setUser_name(userSpecified.get().getUser_name());
            return followedListDto;
        }
        else{
            throw new NotFoundException("User not Found");
        }
    }

    /**
     * US 0007 - Unfollows a user from another user's follower list
     * @param userId - The ID of the user initiating the unfollow request
     * @param userIdToUnfollow - The ID of the user to be unfollowed.
     * @throws BadRequestException If the user attempts to unfollow themselves or
     *                             If they are not currently following the user they intend to unfollow
     * @throws NotFoundException If either the user initiating the unfollow or the user to be unfollowed
     *                           cannot be found
     */
    @Override
    public void setUnfollow(Integer userId, Integer userIdToUnfollow) {
        if (userId.equals(userIdToUnfollow)) {
            throw new BadRequestException("You cannot unfollow yourself.");
        }
        User user = userRepository.getUserById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
        User userToUnfollow = userRepository.getUserById(userIdToUnfollow)
                .orElseThrow(() -> new NotFoundException("User to unfollow not found: " + userIdToUnfollow));
        if (!user.getFollowed().contains(userIdToUnfollow)) {
            throw new BadRequestException("You are not following this user: " + userIdToUnfollow);
        }
        userRepository.updateUserFollowerDelete(user, userToUnfollow);
    }


    /**
     * Call to getFollowerList to sort by name the following users.
     * @param userId
     * @param order
     * @return followerListDto
     */
    @Override
    public FollowerListDto getFollowersOrdered(Integer userId, String order) {
        //Decides order of sorting
        Comparator<String> comparador;
        if(order.equals("name_asc")){
            comparador = Comparator.naturalOrder();
        }
        else {
            comparador = Comparator.reverseOrder();
        }

        //Call to getFollowedList (already exception checked)
        FollowerListDto followerListDto = getFollowerList(userId);
        List<FollowerUsersDto> followerList = followerListDto.getFollowers().stream()
                .sorted(Comparator.comparing(FollowerUsersDto::getUser_name, comparador))
                .toList();



        //Set ordered list
        followerListDto.setFollowers(followerList);

        return followerListDto;
    }
    /**
     * Call to getFollowerList to sort by name the followed users.
     * @param userId
     * @param order
     * @return followerListDto
     */
    @Override
    public FollowListDto getFollowedOrdered(Integer userId, String order) {
        //Decides order of sorting
        Comparator<String> comparador;
        if(order.equals("name_asc")){
            comparador = Comparator.naturalOrder();
        }
        else {
            comparador = Comparator.reverseOrder();
        }
        //Call to getFollowedList (already exception checked)
        FollowListDto followedListDto = getFollowedList(userId);
        List<FollowdUserDto> followerList = followedListDto.getFollowed().stream()
                .sorted(Comparator.comparing(FollowdUserDto::getUser_name, comparador)).toList();

        //Set and return followers
        followedListDto.setFollowed(followerList);

        return followedListDto;
    }

    /**
     * Mapper user to followerUsersDto
     * @param user
     * @return
     */
    @Override
    public FollowerUsersDto convertToFollowUserDto(User user) {
        return new FollowerUsersDto(user.getId(), user.getUser_name());
    }

    /**
     * Returns all users
     * @return
     */
    @Override
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @Override
    public void addPostToUser(Integer userId, Integer postId) {
        User user = userRepository.findUserById(userId);
        if (user == null) {
            throw new NotFoundException("User not found.");
        }
        if(checkPostIdUnique(user.getPosts().stream().toList(),postId)){
            user.getPosts().add(postId);
            userRepository.addPost(userId,postId);
        }else{
            throw new IllegalArgumentException("Post Id is already in the list");
        }
    }

    private Boolean checkPostIdUnique(List<Integer> postList,Integer id){
        for(Integer p:postList){
            if(p.equals(id)){
                return false;
            }
        }
        return true;
    }

}
