/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.rundeck.storage.conf;

import org.rundeck.storage.api.*;
import org.rundeck.storage.api.PathUtil;
import org.rundeck.storage.impl.DelegateResource;
import org.rundeck.storage.impl.DelegateTree;
import org.rundeck.storage.impl.ResourceBase;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * SelectiveTree that Maps resources into a delegate, and can optionally remove the path prefix before transfering
 *
 * This provides a sub path mapped to the root of the delegate.
 *
 * <ul>
 *     <li>Input Path: <pre>${rootPath}/a/b</pre></li>
 *     <li>Path used for delegate: <pre>a/b</pre></li>
 * </ul>
 */
public class SubPathTree<T extends ContentMeta> extends DelegateTree<T> implements SelectiveTree<T> {
    Path rootPath;
    private boolean fullPath;

    /**

     * @param delegate delegate tree
     * @param rootPath root path for the subtree
     * @param fullPath true if the root path should not be removed when accessing the delegate
     */
    public SubPathTree(Tree<T> delegate, String rootPath, boolean fullPath) {
        this(delegate, PathUtil.asPath(rootPath), fullPath);
    }

    public SubPathTree(Tree<T> delegate, Path rootPath, boolean fullPath) {
        super(delegate);
        this.rootPath = rootPath;
        this.fullPath = fullPath;
    }

    @Override
    public Path getSubPath() {
        return rootPath;
    }

    private Path translatePathInternal(Path extpath) {
        return PathUtil.asPath(translatePathInternal(extpath.getPath()));
    }

    /**
     * Translate external path into internal path for the delegate
     * @param extpath externally requested path
     * @return internal path
     */
    protected String translatePathInternal(String extpath) {
        if (fullPath) {
            return extpath;
        } else {
            return PathUtil.removePrefix(rootPath.getPath(), extpath);
        }
    }

    private Path translatePathExternal(Path intpath) {
        return PathUtil.asPath(translatePathExternal(intpath.getPath()));
    }

    /**
     * convert internal path to external
     *
     * @param intpath
     *
     * @return
     */
    protected String translatePathExternal(String intpath) {
        if (fullPath) {
            return intpath;
        } else {
            return PathUtil.appendPath(rootPath.getPath(), intpath);
        }
    }

    @Override
    public boolean hasPath(Path path) {
        return isLocalRoot(path) || super.hasPath(translatePathInternal(path));
    }

    @Override
    public boolean hasResource(Path path) {
        return !isLocalRoot(path) && super.hasResource(translatePathInternal(path));
    }

    @Override
    public boolean hasDirectory(Path path) {
        return isLocalRoot(path) || super.hasDirectory(translatePathInternal(path));
    }

    protected boolean isLocalRoot(Path path) {
        return PathUtil.isRoot(PathUtil.removePrefix(rootPath.getPath(), path.getPath()));
    }

    @Override
    public Resource<T> getResource(Path path) {
        if(isLocalRoot(path)) {
            //root is treated as a dir
            throw new IllegalArgumentException("No resource for path: " + path);
        }
        return translateResourceExternal(super.getResource(translatePathInternal(path)));
    }

    @Override
    public Resource<T> getPath(Path path) {
        if(isLocalRoot(path) && !super.hasDirectory(translatePathInternal(path))) {
            //empty dir
            return translateResourceExternal(new ResourceBase<T>(path, null, true));
        }
        return translateResourceExternal(super.getPath(translatePathInternal(path)));
    }

    @Override
    public Set<Resource<T>> listDirectory(Path path) {
        if(isLocalRoot(path) && !super.hasDirectory(translatePathInternal(path))) {
            return Collections.<Resource<T>>emptySet();
        }
        return translateAllExternal(super.listDirectory(translatePathInternal(path)));
    }

    @Override
    public Set<Resource<T>> listDirectorySubdirs(Path path) {
        if (isLocalRoot(path) && !super.hasDirectory(translatePathInternal(path))) {
            return Collections.<Resource<T>>emptySet();
        }
        return translateAllExternal(super.listDirectorySubdirs(translatePathInternal(path)));
    }

    @Override
    public Set<Resource<T>> listDirectoryResources(Path path) {
        if (isLocalRoot(path) && !super.hasDirectory(translatePathInternal(path))) {
            return Collections.<Resource<T>>emptySet();
        }
        return translateAllExternal(super.listDirectoryResources(translatePathInternal(path)));
    }

    private Set<Resource<T>> translateAllExternal(Set<Resource<T>> internal) {
        HashSet<Resource<T>> resources = new HashSet<Resource<T>>();
        for (Resource<T> resource : internal) {
            resources.add(translateResourceExternal(resource));
        }
        return resources;
    }

    @Override
    public boolean deleteResource(Path path) {
        return super.deleteResource(translatePathInternal(path));
    }

    static class translatedResource<T extends ContentMeta> extends DelegateResource<T> {
        Path newpath;

        translatedResource(Resource<T> delegate, Path newpath) {
            super(delegate);
            this.newpath = newpath;
        }

        @Override
        public Path getPath() {
            return newpath;
        }
    }

    /**
     * Expose a resource with a path that maps to external path
     *
     * @param resource
     *
     * @return
     */
    private Resource<T> translateResourceExternal(Resource<T> resource) {
        if (fullPath) {
            return resource;
        }
        return new translatedResource<T>(resource, translatePathExternal(resource.getPath()));
    }


    @Override
    public Resource<T> createResource(Path path, T data) {
        return translateResourceExternal(super.createResource(translatePathInternal(path), data));
    }

    @Override
    public Resource<T> updateResource(Path path, T data) {
        return translateResourceExternal(super.updateResource(translatePathInternal(path), data));
    }
}
