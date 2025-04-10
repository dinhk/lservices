package com.qut.webservices.igalogicservices.rules.core

import java.util.*

/**
 * Implementation of a topographical sort, lifted from https://www.geeksforgeeks.org/topological-sorting/
 */
object TopographicalSort {
    // Returns adjacency list representation of graph from
    // given set of pairs.
    private fun makeGraph(numTasks: Int, prerequisites: Array<IntArray>): ArrayList<HashSet<Int>> {
        val graph = ArrayList<HashSet<Int>>(numTasks)
        for (i in 0 until numTasks) graph.add(HashSet())
        for (pre in prerequisites) graph[pre[1]].add(pre[0])
        return graph
    }

    // Computes in-degree of every vertex
    private fun computeInDegree(
        graph: ArrayList<HashSet<Int>>
    ): IntArray {
        val degrees = IntArray(graph.size)
        for (neighbors in graph) for (neigh in neighbors) degrees[neigh]++
        return degrees
    }

    // main function for topological sorting
    fun findOrder(numTasks: Int, prerequisites: Array<IntArray>): ArrayList<Int> {
        // Create an adjacency list
        val graph = makeGraph(numTasks, prerequisites)

        // Find vertices of zero degree
        val degrees = computeInDegree(graph)
        val zeros: Queue<Int> = LinkedList()
        for (i in 0 until numTasks) if (degrees[i] == 0) zeros.add(i)

        // Find vertices in topological order
        // starting with vertices of 0 degree
        // and reducing degrees of adjacent.
        val toposort = ArrayList<Int>()
        for (i in 0 until numTasks) {
            if (zeros.isEmpty()) return ArrayList()
            val zero = zeros.peek()
            zeros.poll()
            toposort.add(zero)
            for (neigh in graph[zero]) {
                if (--degrees[neigh] == 0) zeros.add(neigh)
            }
        }
        return toposort
    }
}

