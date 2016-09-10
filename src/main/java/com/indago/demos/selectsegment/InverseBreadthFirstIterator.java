/**
 *
 */
package com.indago.demos.selectsegment;

import net.trackmate.graph.Edge;
import net.trackmate.graph.Graph;
import net.trackmate.graph.Vertex;
import net.trackmate.graph.algorithm.traversal.BreadthFirstIterator;

/**
 * Same as {@link BreadthFirstIterator} on a graph where all directed edges are
 * pointing in the opposite direction.
 * <p>
 * Note, that the set of iterated vertices includes the root vertex.
 *
 * @author jug
 * @author Tobias Pietzsch &lt;tobias.pietzsch@gmail.com&gt;
 */
public class InverseBreadthFirstIterator< V extends Vertex< E >, E extends Edge< V > > extends BreadthFirstIterator< V, E >
{
	/**
	 * @param root
	 * @param graph
	 */
	public InverseBreadthFirstIterator( final V root, final Graph< V, E > graph )
	{
		super( root, graph );
	}

	@Override
	protected Iterable< E > neighbors( final V vertex )
	{
		return vertex.incomingEdges();
	}

	@Override
	protected void fetchNext()
	{
		while ( canFetch() )
		{
			fetched = fetch( fetched );
			for ( final E e : neighbors( fetched ) )
			{
				final V source = e.getSource( tmpRef );
				if ( !visited.contains( source ) )
				{
					visited.add( source );
					toss( source );
				}
			}
			return;
		}
		releaseRef( tmpRef );
		releaseRef( fetched );
		// we cannot release next, because it might still be in used outside of
		// the iterator
		fetched = null;
	}
}
