package com.team4.core.services.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.team4.core.exception.AppException;
import com.team4.core.exception.ErrorCode;
import com.team4.core.services.ImageStorageService;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CloudinaryImageStorageService implements ImageStorageService {
    private final Cloudinary cloudinary;

    @Value("${cloudinary.folder}")
    private String folder;

    @Override
    public String upload(MultipartFile image) {
        if (image == null || image.isEmpty() || image.getContentType() == null
                || !image.getContentType().startsWith("image/")) {
            throw new AppException(ErrorCode.INVALID_ROOM_IMAGE);
        }

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    image.getBytes(),
                    ObjectUtils.asMap("folder", folder, "resource_type", "image"));
            Object secureUrl = result.get("secure_url");
            if (secureUrl == null) {
                throw new AppException(ErrorCode.IMAGE_UPLOAD_FAILED);
            }
            return secureUrl.toString();
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof AppException appException) {
                throw appException;
            }
            throw new AppException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }
}
