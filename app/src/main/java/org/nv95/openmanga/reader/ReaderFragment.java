package org.nv95.openmanga.reader;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.nv95.openmanga.AppBaseFragment;
import org.nv95.openmanga.common.utils.CollectionsUtils;
import org.nv95.openmanga.core.models.MangaPage;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by koitharu on 08.01.18.
 */

public abstract class ReaderFragment extends AppBaseFragment implements ReaderCallback {

	@NonNull
	protected final ArrayList<MangaPage> mPages = new ArrayList<>();
	private boolean mRestored = false;

	@CallSuper
	public void setPages(List<MangaPage> pages) {
		mPages.clear();
		mPages.addAll(pages);
	}

	public ArrayList<MangaPage> getPages() {
		return mPages;
	}

	public abstract int getCurrentPageIndex();

	@Nullable
	public MangaPage getCurrentPage() {
		return CollectionsUtils.getOrNull(mPages, getCurrentPageIndex());
	}

	public abstract void scrollToPage(int index);

	public abstract void smoothScrollToPage(int index);

	public boolean scrollToPageById(long id) {
		for (int i = 0; i < mPages.size(); i++) {
			MangaPage o = mPages.get(i);
			if (o.id == id) {
				scrollToPage(i);
				return true;
			}
		}
		return false;
	}

	@Override
	public void onPageChanged(int page) {
		Activity activity = getActivity();
		if (activity != null && activity instanceof ReaderCallback) {
			((ReaderCallback) activity).onPageChanged(page);
		}
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		final Bundle args = getArguments();
		if (!mRestored && args != null) {
			onRestoreState(args);
			mRestored = true;
		}
	}

	public abstract boolean moveLeft();

	public abstract boolean moveRight();

	public abstract boolean moveUp();

	public abstract boolean moveDown();

	public boolean moveNext() {
		final int newPos = getCurrentPageIndex() + 1;
		if (newPos < mPages.size()) {
			smoothScrollToPage(newPos);
			return true;
		} else {
			return false;
		}
	}

	public boolean movePrevious() {
		final int newPos = getCurrentPageIndex() - 1;
		if (newPos >= 0) {
			smoothScrollToPage(newPos);
			return true;
		} else {
			return false;
		}
	}

	@Override
	@CallSuper
	public void onSaveInstanceState(Bundle outState) {
		final MangaPage page = getCurrentPage();
		if (page != null) {
			outState.putLong("page_id", page.id);
		}
		outState.putParcelableArrayList("pages", mPages);
	}

	@CallSuper
	public void onRestoreState(@NonNull Bundle savedState) {
		ArrayList<MangaPage> pages = savedState.getParcelableArrayList("pages");
		if (pages != null) {
			setPages(pages);
			long pageId = savedState.getLong("page_id", 0);
			if (pageId != 0) {
				scrollToPageById(pageId);
			}
		}
	}

	protected void toggleUi() {
		final Activity activity = getActivity();
		if (activity != null && activity instanceof ReaderActivity) {
			((ReaderActivity) activity).toggleUi();
		}
	}
}
