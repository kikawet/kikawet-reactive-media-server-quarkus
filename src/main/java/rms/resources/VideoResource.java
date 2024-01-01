package rms.resources;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.RestResponse;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import rms.dto.CreateVideoDto;
import rms.exceptions.BadRequestResponseException;
import rms.model.Video;

@Path("video")
public class VideoResource {

    @GET
    public Uni<List<Video>> getAll(@RestQuery Optional<Integer> page, @RestQuery Optional<Integer> pageSize) {
        return Video
                .find("ORDER BY title")
                .page(
                        page
                                .filter(p -> p >= 0)
                                .orElse(0),
                        pageSize
                                .filter(size -> 0 <= size && size <= 50)
                                .orElse(10))
                .list();
    }

    @GET
    @Path("{slug}")
    public Uni<Video> getVideoBySlug(@PathParam("slug") String encodedSlug) {
        String slug;
        try {
            slug = URLDecoder.decode(encodedSlug, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new BadRequestResponseException("Title cannot be decoded from UTF-8");
        }

        Uni<Video> video = Video.find("slug", slug).firstResult();

        return video
                .onItem().ifNull().failWith(new NotFoundException())
                .onItem().ifNotNull().transform((Video v) -> {
                    if (v.isPrivate())
                        throw new NotFoundException();
                    return v;
                });
    }

    @POST
    @RolesAllowed("admin")
    public Uni<RestResponse<Void>> createVideo(@Valid CreateVideoDto videoDto) {
        if (videoDto == null)
            throw new BadRequestResponseException("Must provide body content");

        String encodedTitle;
        try {
            encodedTitle = URLEncoder.encode(videoDto.title(), StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new BadRequestResponseException("Video title cannot be encoded into UTF-8");
        }

        Video video = Video.builder()
                .title(videoDto.title())
                .slug(videoDto.slug())
                .duration(videoDto.duration())
                .isPrivate(videoDto.isPrivate().orElse(Boolean.FALSE))
                .build();

        return Video.findById(videoDto.title())
                .onItem().ifNotNull()
                .failWith(new BadRequestResponseException(
                        "Already exists a video with title: '" + videoDto.title() + "'"))
                .onItem().ifNull().continueWith(video)
                .flatMap(v -> v.persistAndFlush())
                .map(x -> RestResponse.created(URI.create("/video/" + encodedTitle)));
    }

    // @PUT // Used to update if a video is private or not or whom is allowed to see
    // it
    // public void updateVideo(Video video) {
    // throw new UnsupportedOperationException("Method not implemented yet");
    // }
}