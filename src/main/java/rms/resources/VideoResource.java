package rms.resources;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.http.HttpStatus;
import org.jboss.resteasy.reactive.RestResponse;

import io.quarkus.panache.common.Sort;
import io.smallrye.mutiny.Uni;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import rms.dto.CreateVideoDto;
import rms.model.Video;

@Path("video")
public class VideoResource {

    @GET
    public Uni<List<Video>> getAll() {
        return Video.find("title", Sort.by("title")).firstPage().list();
    }

    @GET
    @Path("{title}")
    public Uni<Video> getVideoByTitle(@PathParam("title") String encodedTitle) {
        String title;
        try {
            title = URLDecoder.decode(encodedTitle, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new BadRequestException(Response.status(HttpStatus.SC_BAD_REQUEST).entity(
                    "Title cannot be decoded from UTF-8")
                    .build());
        }

        Uni<Video> video = Video.findById(title);

        return video
                .onItem().ifNull().failWith(new NotFoundException())
                .onItem().ifNotNull().transform((Video v) -> {
                    if (v.isPrivate())
                        throw new NotFoundException();
                    return v;
                });
    }

    @POST
    public Uni<RestResponse<Void>> createVideo(@Valid CreateVideoDto videoDto) {
        if (videoDto == null)
            throw new BadRequestException(Response.status(HttpStatus.SC_BAD_REQUEST).entity(
                    "Must provide body content")
                    .build());

        String encodedTitle;
        try {
            encodedTitle = URLEncoder.encode(videoDto.title(), StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            throw new BadRequestException(Response.status(HttpStatus.SC_BAD_REQUEST).entity(
                    "Video title cannot be encoded into UTF-8")
                    .build());
        }

        Video video = Video.builder()
                .title(videoDto.title())
                .url(videoDto.url())
                .duration(videoDto.duration())
                .isPrivate(videoDto.isPrivate().orElse(Boolean.FALSE))
                .build();

        return Video.findById(videoDto.title())
                .onItem().ifNotNull()
                .failWith(new BadRequestException(Response.status(HttpStatus.SC_BAD_REQUEST).entity(
                        "Already exists a video with title: '" + videoDto.title() + "'")
                        .build()))
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