package com.lesgens.minou.listeners;

public interface AvatarUploadListener {
	public void onAvatarUploaded(final String avatarUrl, final byte[] bytesAvatar);
	public void onAvatarUploadError();
}
