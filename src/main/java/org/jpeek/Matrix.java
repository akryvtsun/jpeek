/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Yegor Bugayenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.jpeek;

import com.jcabi.xml.XMLDocument;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.cactoos.Scalar;
import org.cactoos.collection.Filtered;
import org.cactoos.collection.Joined;
import org.cactoos.iterable.Mapped;
import org.cactoos.scalar.And;
import org.cactoos.scalar.IoCheckedScalar;
import org.xembly.Directive;
import org.xembly.Directives;

/**
 * Matrix.
 *
 * <p>There is no thread-safety guarantee.
 *
 * @author Yegor Bugayenko (yegor256@gmail.com)
 * @version $Id$
 * @since 0.6
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
final class Matrix implements Scalar<Iterable<Directive>> {

    /**
     * Directory to save index to.
     */
    private final Path output;

    /**
     * Ctor.
     * @param target Target dir
     */
    Matrix(final Path target) {
        this.output = target;
    }

    @Override
    public Iterable<Directive> value() throws IOException {
        final SortedMap<String, Map<String, String>> matrix = new TreeMap<>();
        new IoCheckedScalar<>(
            new And(
                new Filtered<>(
                    Files.list(this.output)
                        .collect(Collectors.toList()),
                    path -> path.getFileName()
                        .toString()
                        .matches("^[A-Z].+\\.xml$")
                ),
                path -> {
                    new And(
                        new XMLDocument(path.toFile()).nodes("//class"),
                        node -> {
                            final String name = String.format(
                                "%s.%s",
                                node.xpath("../../package/@id").get(0),
                                node.xpath("@id").get(0)
                            );
                            matrix.putIfAbsent(name, new TreeMap<>());
                            matrix.get(name).put(
                                node.xpath("/metric/title/text()").get(0),
                                node.xpath("@color").get(0)
                            );
                        }
                    ).value();
                }
            )
        ).value();
        return new Directives()
            .add("matrix")
            .append(new Header())
            .add("classes")
            .append(
                new Joined<Directive>(
                    new Mapped<>(
                        matrix.entrySet(),
                        ent -> new Directives().add("class").append(
                            new Joined<Directive>(
                                new Mapped<>(
                                    ent.getValue().entrySet(),
                                    mtd -> new Directives()
                                        .add("metric")
                                        .attr("name", mtd.getKey())
                                        .attr("color", mtd.getValue())
                                        .up()
                                )
                            )
                        ).attr("id", ent.getKey()).up()
                    )
                )
            );
    }

}