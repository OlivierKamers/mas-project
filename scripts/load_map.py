import networkx as nx
import osmgraph
from networkx.drawing.nx_agraph import write_dot


def nodes_tag_filter(tags):
  pass


def ways_tag_filter(tags):
  """
  Filter out roads not accessible to taxis 
  Only keep highway tag (empty tags dict results in way being removed)
  """
  for k in tags.keys():
    if k != 'highway' or tags[k] in ['pedestrian', 'footway', 'cycleway']:
      del tags[k]


def node_coords_to_pos(G):
  coords = nx.get_node_attributes(G, 'coordinate')
  pos = dict(map(lambda (k, v): (k, "{},{}!".format(v[0], v[1])), coords.iteritems()))
  nx.set_node_attributes(G, 'pos', pos)
  nx.set_node_attributes(G, 'p', pos)


def main():
  NAME = 'map'
  g = osmgraph.parse_file('{}.osm'.format(NAME), nodes_tag_filter=nodes_tag_filter, ways_tag_filter=ways_tag_filter)
  print 'Number of edges: {}'.format(len(g.edges()))
  print 'Number of nodes: {}'.format(len(g.nodes()))
  node_coords_to_pos(g)
  write_dot(g, '{}.dot'.format(NAME))


if __name__ == '__main__':
  main()
