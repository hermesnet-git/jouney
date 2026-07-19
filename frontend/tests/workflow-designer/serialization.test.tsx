import { describe, expect, it } from 'vitest'
import { fromWorkflowGraph, toWorkflowGraph, type WorkflowGraph } from '../../src/workflow-designer/serialization/graph'
import type { Edge, Node } from 'reactflow'

describe('toWorkflowGraph / fromWorkflowGraph', () => {
  const nodes: Node<any>[] = [
    { id: 'start-1', position: { x: 0, y: 0 }, data: { label: 'Início', nodeType: 'START', configuration: {} } },
    { id: 'end-1', position: { x: 200, y: 0 }, data: { label: 'Fim', nodeType: 'END', configuration: {} } },
  ]
  const edges: Edge<any>[] = [{ id: 'e1', source: 'start-1', target: 'end-1', data: {} }]

  it('serializes React Flow state into the canonical JSON shape', () => {
    const graph = toWorkflowGraph('k', 'Nome', nodes, edges)

    expect(graph.workflowKey).toBe('k')
    expect(graph.startNodeId).toBe('start-1')
    expect(graph.nodes).toHaveLength(2)
    expect(graph.edges).toEqual([
      { id: 'e1', source: 'start-1', target: 'end-1', condition: undefined, publishEvent: null },
    ])
  })

  it('round-trips a graph through fromWorkflowGraph -> toWorkflowGraph', () => {
    const original: WorkflowGraph = {
      workflowKey: 'k',
      name: 'Nome',
      startNodeId: 'start-1',
      nodes: [
        { id: 'start-1', type: 'START', name: 'Início', configuration: {} },
        { id: 'end-1', type: 'END', name: 'Fim', configuration: {} },
      ],
      edges: [{ id: 'e1', source: 'start-1', target: 'end-1' }],
    }

    const { nodes: rfNodes, edges: rfEdges } = fromWorkflowGraph(original)
    const roundTripped = toWorkflowGraph('k', 'Nome', rfNodes, rfEdges)

    expect(roundTripped.nodes.map((n) => n.id).sort()).toEqual(original.nodes.map((n) => n.id).sort())
    expect(roundTripped.edges).toHaveLength(1)
  })

  it('leaves startNodeId null when there is no START node', () => {
    const graph = toWorkflowGraph('k', 'Nome', [], [])
    expect(graph.startNodeId).toBeNull()
  })
})
